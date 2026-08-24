package scot.gov.publishing.journal;

import org.apache.commons.lang3.RandomStringUtils;
import scot.gov.publishing.jcr.SessionSaver;
import org.apache.commons.lang3.time.StopWatch;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * SearchJournal creating a record of actions that need to be taken in order to index content using funnelback.
 *
 * Maintained under /content/searchjournal
 */
public class Journal {

    private static final Logger LOG = LoggerFactory.getLogger(Journal.class);

    private final Session session;

    private final SessionSaver sessionSaver;

    private static final String CONTENT_ID = "journal:contentid";

    private static final String SITE = "journal:site";

    private static final String ACTION = "journal:action";

    private static final String URL = "journal:url";

    private static final String TIMESTAMP = "journal:timestamp";

    private static final String ATTEMPT = "journal:attempt";

    private static final String SEQUENCE = "journal:sequence";

    public Journal(Session session) {
        this(session, 1);
    }

    public Journal(Session session, int saveInterval) {
        this.session = session;
        this.sessionSaver = new SessionSaver(session, saveInterval);
    }

    public Node record(JournalEntry entry) throws RepositoryException {
        Node record = getNodeForRecord(entry);
        LOG.info("Record journal entry {} {} {}, attempt {}, {}",
                record.getIdentifier(), entry.getAction(), entry.getUrl(), entry.getAttempt(), ((GregorianCalendar) entry.getTimestamp()).toZonedDateTime());
        if (entry.getContentId() != null) {
            record.setProperty(CONTENT_ID, entry.getContentId());
        }
        record.setProperty(SITE, entry.getSite() == null ? "" : entry.getSite());
        record.setProperty(ACTION, entry.getAction().name());
        record.setProperty(URL, entry.getUrl());
        record.setProperty(TIMESTAMP, entry.getTimestamp());
        record.setProperty(ATTEMPT, entry.getAttempt());
        record.setProperty(SEQUENCE, entry.getSequence());
        sessionSaver.save();
        return record;
    }

    public List<JournalEntry> getPendingEntries(Calendar position, long lastSequence, int limit) throws RepositoryException {
        Query query = query(position, lastSequence, limit);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        QueryResult queryResult = query.execute();
        stopWatch.stop();
        List<JournalEntry> entries = new ArrayList<>();
        NodeIterator nodeIterator = queryResult.getNodes();

        GregorianCalendar cal = (GregorianCalendar) position;
        ZonedDateTime zdt = cal.toZonedDateTime();
        LOG.info("GetPendingEntries from journal position {}, {}", zdt, lastSequence);
        while (nodeIterator.hasNext()) {
            Node entryNode = nodeIterator.nextNode();
            JournalEntry entry = entryForNode(entryNode);
            if (entry == null) {
                continue;
            }
            if (includeEntry(entry, position, lastSequence)) {
                entries.add(entry);
            } else {
                LOG.debug("Leaving out entry {} {} {}", entry.getAction(), entry.getUrl(), entry.getSequence());
            }
        }
        // the sort done in the query is at day resolutions for performance reasons, so we sort them here
        entries.sort(Comparator.comparing(JournalEntry::getTimestamp).thenComparing(JournalEntry::getSequence));
        return entries;
    }

    boolean includeEntry(JournalEntry entry, Calendar position, long lastSequence) {
        if (entry.getTimestamp().getTime().getTime() == position.getTime().getTime()) {
            return entry.getSequence() > lastSequence;
        }
        return true;
    }

    Query query(Calendar from, long sequence, int limit) throws RepositoryException {

        // only fetch a maximum of a 6 months of results at a time, this should reduce the memory usage of the query
        Calendar to = (Calendar) from.clone();
        to.add(Calendar.YEAR, 1);

        String toProperty = DateTools.getPropertyForResolution(TIMESTAMP, DateTools.Resolution.DAY);
        String xpath = String.format(
                "//element(*, journal:entry)" +
                        "[@journal:timestamp >= %s]" +
                        "[@%s <= %s] " +
                        "order by @%s, @journal:sequence",
                DateTools.createXPathConstraint(session, from),
                toProperty,
                DateTools.createXPathConstraint(session, to, DateTools.Resolution.DAY),
                toProperty);
        LOG.debug("Journal query: {}", xpath);
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        query.setLimit(limit);
        return query;
    }

    /**
     * Returns {@code null}, logging a warning, if {@code node} is missing any required property
     * (e.g. an older node predating a namespace change) or has an unrecognised action value, so
     * that one malformed entry doesn't fail the whole reconciliation batch.
     */
    JournalEntry entryForNode(Node node) throws RepositoryException {
        if (!hasRequiredProperties(node)) {
            LOG.warn("Journal entry {} is missing a required property, skipping", node.getPath());
            return null;
        }
        JournalEntry entry = new JournalEntry();
        if (node.hasProperty(CONTENT_ID)) {
            entry.setContentId(node.getProperty(CONTENT_ID).getString());
        }
        if (node.hasProperty(SITE)) {
            entry.setSite(node.getProperty(SITE).getString());
        }
        try {
            entry.setAction(JournalAction.fromBloomreachAction(node.getProperty(ACTION).getString()));
        } catch (IllegalArgumentException e) {
            LOG.warn("Journal entry {} has an unrecognised action '{}', skipping",
                    node.getPath(), node.getProperty(ACTION).getString());
            return null;
        }
        entry.setUrl(node.getProperty(URL).getString());
        entry.setTimestamp(node.getProperty(TIMESTAMP).getDate());
        entry.setAttempt(node.getProperty(ATTEMPT).getLong());
        entry.setSequence(node.getProperty(SEQUENCE).getLong());
        return entry;
    }

    boolean hasRequiredProperties(Node node) throws RepositoryException {
        return node.hasProperty(ACTION)
                && node.hasProperty(URL)
                && node.hasProperty(TIMESTAMP)
                && node.hasProperty(ATTEMPT)
                && node.hasProperty(SEQUENCE);
    }

    Node getNodeForRecord(JournalEntry entry) throws RepositoryException {
        Node content = session.getNode("/content");
        Node searchjournal = ensurePathNode(content, "searchjournal");
        Calendar date = entry.getTimestamp();
        Node year = ensurePathNode(searchjournal, Integer.toString(date.get(Calendar.YEAR)));
        Node month = ensurePathNode(year, Integer.toString(date.get(Calendar.MONTH)));
        Node day = ensurePathNode(month, Integer.toString(date.get(Calendar.DAY_OF_MONTH)));
        String newName = uniquename(day);
        return day.addNode(newName, "journal:entry");
    }

    Node ensurePathNode(Node parent, String name) throws RepositoryException {
        if (parent.hasNode(name)) {
            return parent.getNode(name);
        }
        return parent.addNode(name, "nt:unstructured");
    }

    String uniquename(Node parent) throws RepositoryException {
        String candidate = RandomStringUtils.randomAlphabetic(4);
        return parent.hasNode(candidate) ? uniquename(parent) : candidate;
    }

    public Session getSession() {
        return session;
    }
}
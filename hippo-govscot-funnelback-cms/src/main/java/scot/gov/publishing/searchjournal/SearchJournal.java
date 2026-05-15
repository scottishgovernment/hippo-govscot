package scot.gov.publishing.searchjournal;

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
public class SearchJournal {

    private static final Logger LOG = LoggerFactory.getLogger(SearchJournal.class);

    private final Session session;

    private final SessionSaver sessionSaver;

    private static final String ACTION = "searchjournal:action";

    private static final String COLLECTION = "searchjournal:collection";

    private static final String URL = "searchjournal:url";

    private static final String TIMESTAMP = "searchjournal:timestamp";

    private static final String ATTEMPT = "searchjournal:attempt";

    private static final String SEQUENCE = "searchjournal:sequence";

    public SearchJournal(Session session) {
        this(session, 1);
    }

    public SearchJournal(Session session, int saveInterval) {
        this.session = session;
        this.sessionSaver = new SessionSaver(session, saveInterval);
    }

    public Node record(SearchJournalEntry entry) throws RepositoryException {
        Node record = getNodeForRecord(entry);
        LOG.info("record journal entry {} {} {} {}, attempt {}, {}",
                record.getIdentifier(), entry.getAction(), entry.getCollection(), entry.getUrl(), entry.getAttempt(), ((GregorianCalendar) entry.getTimestamp()).toZonedDateTime());
        record.setProperty(ACTION, entry.getAction());
        record.setProperty(COLLECTION, entry.getCollection());
        record.setProperty(URL, entry.getUrl());
        record.setProperty(TIMESTAMP, entry.getTimestamp());
        record.setProperty(ATTEMPT, entry.getAttempt());
        record.setProperty(SEQUENCE, entry.getSequence());
        sessionSaver.save();
        return record;
    }

    public List<SearchJournalEntry> getPendingEntries(Calendar position, long lastSequence, int limit) throws RepositoryException {
        Query query = query(position, lastSequence, limit);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        QueryResult queryResult = query.execute();
        stopWatch.stop();
        LOG.info("getPendingEntries took {}", stopWatch.getTime());
        List<SearchJournalEntry> entries = new ArrayList<>();
        NodeIterator nodeIterator = queryResult.getNodes();

        GregorianCalendar cal = (GregorianCalendar) position;
        ZonedDateTime zdt = cal.toZonedDateTime();
        LOG.info("getPendingEntries {}, {}", zdt, lastSequence);
        while (nodeIterator.hasNext()) {
            Node entryNode = nodeIterator.nextNode();
            SearchJournalEntry entry = entryForNode(entryNode);
            if (includeEntry(entry, position, lastSequence)) {
                entries.add(entry);
            } else {
                LOG.info("leaving out entry {} {} {}", entry.getAction(), entry.getUrl(), entry.getSequence());
            }
        }
        // the sort done in the query is at day resolutions for performance reasons, so we sort them here
        entries.sort(Comparator.comparing(SearchJournalEntry::getTimestamp).thenComparing(SearchJournalEntry::getSequence));
        return entries;
    }

    boolean includeEntry(SearchJournalEntry entry, Calendar position, long lastSequence) {
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
                "//element(*, searchjournal:entry)" +
                        "[@searchjournal:timestamp >= %s]" +
                        "[@%s <= %s] " +
                        "order by @%s, @searchjournal:sequence",
                DateTools.createXPathConstraint(session, from),
                toProperty,
                DateTools.createXPathConstraint(session, to, DateTools.Resolution.DAY),
                toProperty);
        LOG.info("journal query: {}", xpath);
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH);
        query.setLimit(limit);
        return query;
    }

    SearchJournalEntry entryForNode(Node node) throws RepositoryException {
        SearchJournalEntry entry = new SearchJournalEntry();
        entry.setAction(node.getProperty(ACTION).getString());
        entry.setCollection(node.getProperty(COLLECTION).getString());
        entry.setUrl(node.getProperty(URL).getString());
        entry.setTimestamp(node.getProperty(TIMESTAMP).getDate());
        entry.setAttempt(node.getProperty(ATTEMPT).getLong());
        entry.setSequence(node.getProperty(SEQUENCE).getLong());
        return entry;
    }

    Node getNodeForRecord(SearchJournalEntry entry) throws RepositoryException {
        Node content = session.getNode("/content");
        Node searchjournal = ensurePathNode(content, "searchjournal");
        Calendar date = entry.getTimestamp();
        Node year = ensurePathNode(searchjournal, Integer.toString(date.get(Calendar.YEAR)));
        Node month = ensurePathNode(year, Integer.toString(date.get(Calendar.MONTH)));
        Node day = ensurePathNode(month, Integer.toString(date.get(Calendar.DAY_OF_MONTH)));
        String newName = uniquename(day);
        return day.addNode(newName, "searchjournal:entry");
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
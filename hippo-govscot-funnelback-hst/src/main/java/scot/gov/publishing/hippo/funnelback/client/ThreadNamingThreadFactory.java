package scot.gov.publishing.hippo.funnelback.client;


import org.apache.commons.lang.RandomStringUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class ThreadNamingThreadFactory implements ThreadFactory {

    private String prefix;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setName(prefix + RandomStringUtils.randomNumeric(3));
        return t;
    }
}

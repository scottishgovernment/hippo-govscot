package scot.gov.publishing.health;

import java.util.ArrayList;
import java.util.List;

public class Health {

    NagiosStatus status;

    String message;

    String performanceData = "";

    List<String> info = new ArrayList<String>();

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NagiosStatus getStatus() {
        return status;
    }

    public void setStatus(NagiosStatus status) {
        this.status = status;
    }

    public String getPerformanceData() {
        return performanceData;
    }

    public void setPerformanceData(String performanceData) {
        this.performanceData = performanceData;
    }

    public List<String> getInfo() {
        return info;
    }

    public void setInfo(List<String> info) {
        this.info = info;
    }

}

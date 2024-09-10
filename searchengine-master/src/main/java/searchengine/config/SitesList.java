package searchengine.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Setter
@Component
@ConfigurationProperties(prefix = "indexing-settings")
public class SitesList {

    private List<Site> sites;

    public List<Site> getSites() {
        for (Site site : sites) {
            String url = site.getUrl();
            if (url.lastIndexOf("/") != url.length() - 1) {
                url = url + "/";
                site.setUrl(url);
            }
        }
        return sites;
    }
}

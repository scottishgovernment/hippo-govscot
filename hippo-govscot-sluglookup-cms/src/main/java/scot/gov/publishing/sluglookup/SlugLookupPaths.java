package scot.gov.publishing.sluglookup;

public class SlugLookupPaths {

    private SlugLookupPaths() {
        // private constructor
    }

    public static String slugLookupPath(String slug, String site, String type, String mount) {
        String[] hashPath = slugHashPath(slug);
        StringBuilder b = new StringBuilder("/content/urls/")
                .append(site).append('/')
                .append(type).append('/')
                .append(mount).append('/');
        for (String pathPart : hashPath) {
            b.append(pathPart).append('/');
        }
        return b.append(slug).toString();
    }

    public static String[] slugHashPath(String slug) {
        int hash = slug.hashCode();
        String hashString = String.format("%04x", hash & 0xFFFF);
        return new String[] {
                hashString.substring(0, 1),
                hashString.substring(1, 2),
                hashString.substring(2, 3),
                hashString.substring(3, 4)
        };
    }

}
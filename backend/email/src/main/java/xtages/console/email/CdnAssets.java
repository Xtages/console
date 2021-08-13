package xtages.console.email;

import java.net.URI;

public class CdnAssets {
    private CdnAssets() {
    }

    private static final String cdnDomain = "dqcve3avsmxqw.cloudfront.net";

    private static final String imgUrl = "https://" + cdnDomain + "/images";

    public static final URI logoUrl = URI.create(imgUrl + "/logo-email.b7e29b8522d7664e0c54a5dc591d17eaf2406d13.png");
    public static final URI thumbsUpImgUrl = URI.create(imgUrl + "/thumbs-up.0c9c1e4c364e7e8ea9b5dd240e8c801a2923c60c.png");
    public static final URI thumbsDownImgUrl = URI.create(imgUrl + "/thumbs-down.34103e505b97cd1c486dcc26eee345850deb48a7.png");
}

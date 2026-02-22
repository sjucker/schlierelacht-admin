package ch.schlierelacht.admin.views.util;

import ch.schlierelacht.admin.service.CloudflareService;
import com.vaadin.flow.component.html.Image;

public class CloudflareImage extends Image {

    public CloudflareImage(CloudflareService cloudflareService, String cloudflareId, String altText) {
        super(cloudflareService.getImageDeliveryUrl(cloudflareId), altText);
    }
}

package yeuni.co.tz.imageselector;

import com.google.gson.annotations.SerializedName;

public class ImageResponse {
    @SerializedName("url")
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }
}

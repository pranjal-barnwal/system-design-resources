
interface IImage {
    void display();
}

class RealImage implements IImage {
    private String filename;

    public RealImage(String file) {
        this.filename = file;
        System.out.println("[RealImage] Loading image from disk: " + filename);
    }

    @Override
    public void display() {
        System.out.println("[RealImage] Displaying " + filename);
    }
}

//. inheriting from the same interface, so we can use this proxy in place of the real image without the client needing to know about it
class ImageProxy implements IImage {
    private RealImage realImage;
    private String filename;

    public ImageProxy(String file) {
        this.filename = file;
        // since object creation is expensive, 
        // we delay it until it's actually needed with this Proxy pattern
        this.realImage = null;
    }
    
    @Override
    public void display() {
        if (realImage == null) {
            realImage = new RealImage(filename);
            // apply some compression or caching logic here if needed
        }
        realImage.display();
    }
}

public class VirtualProxy {
    public static void main(String[] args) {
        IImage image1 = new ImageProxy("sample.jpg");
        System.out.println("Image created, but not loaded yet.");
        image1.display();
    }
}

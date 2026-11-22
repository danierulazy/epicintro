package id.danierulazy.acediamcintro;

public class MemberEntry {
    private final int index;
    private final String username;
    private final String displayName;
    private final String quote;
    private final String animation;
    private final String handItem;
    private final String particleTheme;

    public MemberEntry(int index, String username, String displayName, String quote,
                       String animation, String handItem, String particleTheme) {
        this.index = index;
        this.username = username;
        this.displayName = displayName;
        this.quote = quote;
        this.animation = animation;
        this.handItem = handItem;
        this.particleTheme = particleTheme;
    }

    public int getIndex() { return index; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getQuote() { return quote; }
    public String getAnimation() { return animation; }
    public String getHandItem() { return handItem; }
    public String getParticleTheme() { return particleTheme; }
}

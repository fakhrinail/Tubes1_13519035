package za.co.entelect.challenge.command;

public class BananaBombCommand implements Command {

    private final int x;
    private final int y;
    // private final int id;
    private final String s;

    public BananaBombCommand(int x, int y, String s) {
        this.x = x;
        this.y = y;
        this.s = s;
    }

    // Public String select(int id) {
    //     if (this.id != 1 || ) {
            
    //     } else {
            
    //     }
    //     return String.format(";select %d;", id);
    // }

    @Override
    public String render() {
        return String.format("%sbanana %d %d", s, x, y);
    }
}

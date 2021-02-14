package za.co.entelect.challenge.command;

import za.co.entelect.challenge.enums.Direction;

public class ShootCommand implements Command {

    private Direction direction;
    private final String s;

    public ShootCommand(Direction direction,String s) {
        this.direction = direction;
        this.s = s;
    }

    @Override
    public String render() {
        return String.format("%sshoot %s",s, direction.name());
    }
}

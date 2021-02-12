package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    public Command run() {

        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null) { 
            if (canSnowball(enemyWorm)) return new SnowballCommand(enemyWorm.position.x, enemyWorm.position.y);
            else if (canBananaBombs()) return new BananaBombCommand(enemyWorm.position.x, enemyWorm.position.y);
            else {
                Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
                return new ShootCommand(direction);
            }
        }
        
        Worm nearestWorm = getNearestWorm();
        int moveX = currentWorm.position.x;
        int moveY = currentWorm.position.y;
        int enemySubCurrX = nearestWorm.position.x-currentWorm.position.x;
        int enemySubCurrY = nearestWorm.position.y-currentWorm.position.y;

        if (enemySubCurrX > 0) moveX = currentWorm.position.x+1;
        else if (enemySubCurrX < 0) moveX = currentWorm.position.x-1;
        else if (enemySubCurrX == 0) moveX = currentWorm.position.x;

        if (enemySubCurrY > 0) moveY = currentWorm.position.y+1;
        else if (enemySubCurrY < 0) moveY = currentWorm.position.y-1;
        else if (enemySubCurrY == 0) moveY = currentWorm.position.y;

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        // Cell chosenCell = null;
        for (Cell cell : surroundingBlocks) {
            if (cell.x == moveX && cell.y == moveY) {
                // chosenCell = cell;
                if (cell.type == CellType.DIRT) {
                    return new DigCommand(moveX, moveY);
                } else if (cell.type == CellType.AIR || cell.type == CellType.LAVA) {
                    return new MoveCommand(moveX, moveY);
                }
                break;
            }
        }
        
        return new DoNothingCommand();
    }

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());

        Worm nearestWorm = getNearestWorm();

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0 && enemyWorm == nearestWorm) {
                return enemyWorm;
            }
        }

        return null;
    }

    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX, coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }
                // if(currentWorm.id == 2 && currentWorm.bananaBombs.count == 0) break;
                // else if (currentWorm.id == 3 && currentWorm.snowballs.count == 0) break;
                // else if (cell.type != CellType.AIR) break;

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if ((i != x || j != y) && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize
                && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private boolean canSnowball(Worm enemyWorm) {
        return (currentWorm.id == 3) && (currentWorm.snowballs.count > 0) && (enemyWorm.roundsUntilUnfrozen <= 1);
        // masih liat bisa snowball atau sisa ngganya belom distance
    }

    private boolean canBananaBombs() {
        return (currentWorm.id == 2) && (currentWorm.bananaBombs.count > 0);
        // masih liat bisa snowball atau sisa ngganya belom distance
    }

    private Worm getNearestWorm() {
        Worm nearestWorm = null;
        int nearestDistance, tempDistance;
        nearestDistance = 999999;

        for (Worm enemyWorm : opponent.worms) {
            tempDistance = euclideanDistance(enemyWorm.position.x, enemyWorm.position.y, currentWorm.position.x, currentWorm.position.y);
            if (tempDistance < nearestDistance && enemyWorm.health > 0) {
                nearestDistance = tempDistance;
                nearestWorm = enemyWorm;
            }
        }

        return nearestWorm;
    }
}

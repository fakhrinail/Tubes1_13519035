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
        Worm inRangeWorm = getFirstWormInRange();
        Worm nearestWorm = getNearestWorm();
        Cell nearestPowerUp = getNearestPowerUp();

        if (inRangeWorm != null) return attackNearest(inRangeWorm);
        else return moveNearestEntity(nearestWorm, nearestPowerUp);
    }

    //mendapatkan worm in range
    private Worm getFirstWormInRange() {
        Set<String> cells;
        Set<String> cellsAdd;
        int range = currentWorm.weapon.range;

        cells = constructFireDirectionLines(range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());
        
        if (canSnowball()) {
            range = currentWorm.snowballs.range;
            cellsAdd = constructBananaSnowballDirectionLines(range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());
            cells.addAll(cellsAdd);
        }
        else if (canBananaBombs()) {
            range = currentWorm.bananaBombs.range;
            cellsAdd = constructBananaSnowballDirectionLines(range)
                .stream()
                .flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y))
                .collect(Collectors.toSet());
            cells.addAll(cellsAdd);
        }
        
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
                
                if (currentWorm.id == 2 && currentWorm.bananaBombs.count == 0 && cell.type == CellType.DIRT) {
                    break;
                }
                else if (currentWorm.id == 3 && currentWorm.snowballs.count == 0 && cell.type == CellType.DIRT) {
                    break;
                }
                else if (currentWorm.id == 1 && cell.type == CellType.DIRT) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    // counstruct line untuk banabombs dan snowball
    private List<List<Cell>> constructBananaSnowballDirectionLines(int range) {
        List<List<Cell>> theCells = new ArrayList<>();
        int currX = currentWorm.position.x;
        int currY = currentWorm.position.y;

        for (int i = currX-range; i <= currX+range; i++) {
            List<Cell> theCell = new ArrayList<>();
            for (int j = currY-range; j <= currY+range; j++) {
                if (isValidCoordinate(i,j)) {
                    Cell cell = gameState.map[i][j];
                    if ((i!=currX || j!=currY) && euclideanDistance(currentWorm.position.x,currentWorm.position.y, cell.x, cell.y) <= 5) {
                        theCell.add(cell);
                    }
                }
            }     
            theCells.add(theCell);
        }

        return theCells;
    }

    //mendapatkan cell disekitar currentworm
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

    //mendapatkan jarak antar titik
    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    //mengecek apakah suatu koordinat valid
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

    //mengecek apakah currentworm bisa snowballs
    private boolean canSnowball() {
        return (currentWorm.id == 3) && (currentWorm.snowballs.count > 0);
    }

    //mengecek apakah currentworm bisa banana bombs
    private boolean canBananaBombs() {
        return (currentWorm.id == 2) && (currentWorm.bananaBombs.count > 0);
    }

    //mendapat worm terdekat dari currentworm
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

    //mengattack worm terdekat
    private Command attackNearest(Worm enemyWorm) {
        if (canSnowball()) return new SnowballCommand(enemyWorm.position.x, enemyWorm.position.y);
        else if (canBananaBombs()) return new BananaBombCommand(enemyWorm.position.x, enemyWorm.position.y);
        else {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }
    }

    //gerak menuju x,y
    private Command moveNearest(int x, int y) {
        int moveX = currentWorm.position.x;
        int moveY = currentWorm.position.y;
        int subCurrX = x-currentWorm.position.x;
        int subCurrY = y-currentWorm.position.y;

        if (subCurrX > 0) moveX = moveX+1;
        else if (subCurrX < 0) moveX = moveX-1;

        if (subCurrY > 0) moveY = moveY+1;
        else if (subCurrY < 0) moveY = moveY-1;

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        for (Cell cell : surroundingBlocks) {
            if (cell.x == moveX && cell.y == moveY) {
                if (cell.type == CellType.DIRT) {
                    return new DigCommand(moveX, moveY);
                } else if (cell.type == CellType.AIR || cell.type == CellType.LAVA) {
                    if(!(isOccupied(cell))) return new MoveCommand(moveX, moveY);
                }
                break;
            }
        }
        
        return new DoNothingCommand();
    }
    
    //mendapatkan powerup terdekat jika masih ada
    private Cell getNearestPowerUp() {
        int tempDistance, nearestDistance;
        Cell nearestPowerUp = null;
        nearestDistance = 999999;

        for (int i = 0; i < gameState.mapSize; i++){
            for (int j = 0; j < gameState.mapSize; j++) {
                if (gameState.map[i][j].powerUp != null){
                    tempDistance = euclideanDistance(gameState.map[i][j].x, gameState.map[i][j].y, currentWorm.position.x, currentWorm.position.y);
                    if (tempDistance < nearestDistance){
                        nearestDistance = tempDistance;
                        nearestPowerUp = gameState.map[i][j];
                    }
                }
            }
        }

        return nearestPowerUp;
    }

    //gerak menuju entitas terdekat
    private Command moveNearestEntity(Worm nearestWorm, Cell nearestPowerUp){
        if (nearestPowerUp != null) {
            boolean isWormNearest;
            int nearestWormDistance = euclideanDistance(nearestWorm.position.x, nearestWorm.position.y, currentWorm.position.x, currentWorm.position.y);
            int nearestPowerUpDistance = euclideanDistance(nearestPowerUp.x, nearestPowerUp.y, currentWorm.position.x, currentWorm.position.y);
            isWormNearest = nearestWormDistance >= nearestPowerUpDistance;
            if (isWormNearest) return moveNearest(nearestWorm.position.x, nearestWorm.position.y);
            else return moveNearest(nearestPowerUp.x, nearestPowerUp.y);
        }
        return moveNearest(nearestWorm.position.x, nearestWorm.position.y);
    }

    //mengecek apakah suatu cell occupied atau tidak
    private boolean isOccupied(Cell cell) {
        return (cell.occupier != null);
    }
}

package lightUpGame;

import java.util.Set;

class ComponentScore {
    int id;
    Set<Point> component;
    int score;
    
    ComponentScore(int id, Set<Point> component, int score) {
        this.id = id;
        this.component = component;
        this.score = score;
    }
}
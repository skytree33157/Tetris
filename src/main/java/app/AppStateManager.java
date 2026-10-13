package app;

public class AppStateManager {
    private AppState currentState;

    public AppStateManager(AppState initialState) {
        this.currentState = initialState;
    }

    public AppState getCurrentState() {
        return currentState;
    }

    public void transitionTo(AppState next){
        this.currentState = next;
    }
}

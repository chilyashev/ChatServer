package server.ui;

/**
 * An interface for the screens used in here
 * Date: 4/20/14 2:16 AM
 *
 * @author Mihail Chilyashev
 */
public interface ControlledScreen {
    public void setParent(ScreenContainer screen);

    public void init();

    void close();
}

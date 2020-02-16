package de.tum.i13.shared;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class inputPassword {

    private int countPasswordInput = 0;
    private String[] prevCommand = new String[]{};
    private boolean inputPassword = false;

    public synchronized int getCountPasswordInput() {
        return countPasswordInput;
    }

    public synchronized void setCountPasswordInput(int countPasswordInput) {
        this.countPasswordInput = countPasswordInput;
    }

    public synchronized boolean isInputPassword() {
        return inputPassword;
    }

    public synchronized void setInputPassword(boolean inputPassword) {
        this.inputPassword = inputPassword;
    }

    public inputPassword(boolean inputPassword, int countPasswordInput) {
        this.inputPassword = inputPassword;
        this.countPasswordInput = countPasswordInput;
    }


    public synchronized void increaseCounter() {
        this.countPasswordInput++;
    }

    public synchronized void setPrevCommand(String[] command) {
        this.prevCommand = new String[]{};
        this.prevCommand = command;
    }

    public synchronized String[] getPrevCommand() {
        return prevCommand;
    }

    public synchronized void clearPrevCommand() {
        this.prevCommand = new String[]{};
    }
}

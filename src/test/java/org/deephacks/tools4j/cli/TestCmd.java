package org.deephacks.tools4j.cli;

import javax.validation.constraints.NotNull;

public class TestCmd {
    public static void main(String[] args) {
        CliMain main = new CliMain(new String[] { "ls", "-o", "opt1", "--opt2", "1" });
        main.run(new TestCmd());
    }

    @CliOption(shortName = "o")
    private String opt1;
    @CliOption(shortName = "p")
    @NotNull
    private Integer opt2;

    /**
     * List files in a directory
     * @param ls the directory to list
     */
    @CliCmd
    private void ls(@Default("sd") Integer ls, @NotNull Double test) {
        System.out.println("ls " + ls);
        System.out.println("opt1 " + opt1);
        System.out.println("opt2 " + opt2);
        System.out.println("test " + test);
    }

    /**
     * Remove a file from disk. 
     * @param rm file to be removed
     */
    @CliCmd
    private void rm(String rm) {
        System.out.println("rm " + rm);
        System.out.println("opt1 " + opt1);
        System.out.println("opt2 " + opt2);
    }

    /**
     * Touch a file and modify its time stamp.
     * 
     * @param touch file to touch
     */
    @CliCmd
    private void touch(String touch) {
        System.out.println("touch " + touch);
        System.out.println("opt1 " + opt1);
        System.out.println("opt2 " + opt2);
    }

    private void thisIsNotACommand() {

    }

}

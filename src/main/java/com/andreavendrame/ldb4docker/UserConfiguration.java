package com.andreavendrame.ldb4docker;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table( name = "userconfiguration")
public class UserConfiguration implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String bigraphFilePath;
    private String commandLineArgs = "";


    // Opzione numero 1: ...
    // Opzione numero 2: ...
    // Opzione n: ...



    public UserConfiguration(String bigraphFilePath) {
        this.bigraphFilePath = bigraphFilePath;
        System.out.println("Creata una nuova configurazione con file al percorso: " + bigraphFilePath);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBigraphFilePath() {
        return bigraphFilePath;
    }

    public UserConfiguration() {
    }

    public String getCommandLineArgs() {
        return commandLineArgs;
    }

    public void setCommandLineArgs(String commandLineArgs) {
        this.commandLineArgs = commandLineArgs;
    }

    public void setBigraphFilePath(String bigraphFilePath) {
        this.bigraphFilePath = bigraphFilePath;
        this.commandLineArgs = "";
    }
}

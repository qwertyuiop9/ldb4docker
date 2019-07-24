package com.andreavendrame.ldb4docker;

import java.util.LinkedList;
import java.util.List;

/**
 * Questa classe contiene solo metodi per eseguire debugging
 */
public class MyDebug {

    public static void myPrint(String[] strings) {

        for (String s : strings) {
            System.out.println("Stringa " + s);
        }

    }

    public static List<UserConfiguration> getTestUserConfigurations() {

        List<UserConfiguration> configurations = new LinkedList<>();
        UserConfiguration.Builder builder = new UserConfiguration.Builder();
        UserConfiguration one = builder.startBuilding()
                .setBigraphPath("Percorso_di_rete_1.yml")
                .setId(1)
                .setDescription("Descrizione per il primo bigrafo...").build();
        configurations.add(one);
        configurations.add(builder.startBuilding()
                .setBigraphPath("Percorso_di_rete_2.yml")
                .setId(2)
                .setDescription("Descrizione del secondo bigrafo").build());
        configurations.add(builder.startBuilding()
                .setBigraphPath("Percorso_locale_3.yml")
                .setId(3)
                .setDescription("Descrizione bigrafo numero 3").build());
        return configurations;
    }
}

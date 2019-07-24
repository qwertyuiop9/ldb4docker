package com.andreavendrame.ldb4docker;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table( name = "userconfiguration")
class UserConfiguration implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private String bigraphFilePath;
    private String description;

    private UserConfiguration() {}

    public String getDescription() {
        return description;
    }

    private void setDescription(String description) { this.description = description; }

    public Integer getId() {
        return id;
    }

    private void setId(Integer id) {
        this.id = id;
    }

    public String getBigraphFilePath() {
        return bigraphFilePath;
    }

    private void setBigraphFilePath(String bigraphFilePath) {
        this.bigraphFilePath = bigraphFilePath;
    }

    public static class Builder {

        private static UserConfiguration userConfiguration;
        private static boolean isBuilding = false;

        /**
         * This method must be the first method called (after the constructor)
         * @return a builder to build an instance of UserConfiguration
         */
        public Builder startBuilding() {

            userConfiguration = new UserConfiguration();
            isBuilding = true;
            return this;
        }

        /**
         *
         * @param myBigraphPath the absolut path to my bigraph
         * @return the builder object
         */
        public Builder setBigraphPath(String myBigraphPath) {

            if (isBuilding) {
                userConfiguration.setBigraphFilePath(myBigraphPath);
                return this;
            } else {
                throw new NullPointerException("DEBUG - The builder must be initialized first with the method 'startBuilding'");
            }

        }

        /**
         *
         * @param customId an integer that must be different from each of the other bigraph
         * @return the builder
         */
        public Builder setId(Integer customId) {

            if (isBuilding) {
                userConfiguration.setId(customId);
                return this;
            } else {
                throw new NullPointerException("DEBUG - The builder must be initialized first with the method 'startBuilding'");
            }

        }

        /**
         *
         * @param myDescription a string that describes the bigraph or add information
         * @return the builder
         */
        public Builder setDescription(String myDescription) {

            if (isBuilding) {
                userConfiguration.setDescription(myDescription);
                return this;
            } else {
                throw new NullPointerException("DEBUG - The builder must be initialized first with the method 'startBuilding'");
            }
        }

        /**
         *
         * @return an instance of the class UserConfiguration
         */
        public UserConfiguration build() {

            if (isBuilding) {
                isBuilding = false;
                return userConfiguration;
            } else {
                throw new NullPointerException("DEBUG - The builder must be initialized first with the method 'startBuilding'");
            }
        }
    }
}
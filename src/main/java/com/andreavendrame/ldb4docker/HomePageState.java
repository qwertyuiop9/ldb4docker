package com.andreavendrame.ldb4docker;

public class HomePageState {

    private String bigraphFilePathYml;
    private String securityLevelsFilePath;
    private boolean cbUseProvidedYmlFilePath = false;
    private boolean cbDoInitialLinkCheck = false;
    private boolean cbDoInitialSecurityLevelsCheck = false;

    public HomePageState() {
        System.out.println("Creato l'oggetto stato della pagina");
    }

    public String getBigraphFilePathYml() {
        return bigraphFilePathYml;
    }

    public void setBigraphFilePathYml(String bigraphFilePathYml) {
        this.bigraphFilePathYml = bigraphFilePathYml;
    }

    public String getSecurityLevelsFilePath() {
        return securityLevelsFilePath;
    }

    public void setSecurityLevelsFilePath(String securityLevelsFilePath) {
        this.securityLevelsFilePath = securityLevelsFilePath;
    }

    public boolean isCbUseProvidedYmlFilePath() {
        return cbUseProvidedYmlFilePath;
    }

    public void setCbUseProvidedYmlFilePath(boolean cbUseProvidedYmlFilePath) {
        this.cbUseProvidedYmlFilePath = cbUseProvidedYmlFilePath;
    }

    public boolean isCbDoInitialLinkCheck() {
        return cbDoInitialLinkCheck;
    }

    public void setCbDoInitialLinkCheck(boolean cbDoInitialLinkCheck) {
        this.cbDoInitialLinkCheck = cbDoInitialLinkCheck;
    }

    public boolean isCbDoInitialSecurityLevelsCheck() {
        return cbDoInitialSecurityLevelsCheck;
    }

    public void setCbDoInitialSecurityLevelsCheck(boolean cbDoInitialSecurityLevelsCheck) {
        this.cbDoInitialSecurityLevelsCheck = cbDoInitialSecurityLevelsCheck;
    }

    /**
     *
     * @param filePath percorso di rete che identifica il file contenente il bigrafo
     * @return true se il file in questione contiene dati per un bigrafo valido
     */
    public boolean isValidYml(String filePath) {
        // Implementazione da inserire in base alle necessità
        return true;
    }

    /**
     *
     * @param filePath percorso di rete che identifica il file contenente i livelli di sicurezza da analizzare
     * @return true se il file in questione contiene dati validi
     */
    public boolean isValidSecurityLevelFile(String filePath) {
        // Implementazione da inserire in base alle necessità
        return true;
    }
}

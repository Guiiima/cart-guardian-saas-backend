package com.cartguardian.backend.model;

public class CampanhaRecuperacao {

    private boolean ativa;
    private String lojaId;
    private String templateEmail;
    private long tempoEsperaMin;

    // Construtor vazio é necessário para o Firestore
    public CampanhaRecuperacao() {}

    // Getters e Setters
    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public String getLojaId() {
        return lojaId;
    }

    public void setLojaId(String lojaId) {
        this.lojaId = lojaId;
    }

    public String getTemplateEmail() {
        return templateEmail;
    }

    public void setTemplateEmail(String templateEmail) {
        this.templateEmail = templateEmail;
    }

    public long getTempoEsperaMin() {
        return tempoEsperaMin;
    }

    public void setTempoEsperaMin(long tempoEsperaMin) {
        this.tempoEsperaMin = tempoEsperaMin;
    }
}
package whatsut.common;

public enum StatusUsuario {
    ONLINE("Online"),
    AUSENTE("Ausente"),
    OCUPADO("Ocupado"),
    OFFLINE("Offline");

    private final String rotulo;
    StatusUsuario(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}

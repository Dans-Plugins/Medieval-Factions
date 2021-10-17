package dansplugins.factionsystem.objects.specification.generic;

public interface Territory {
    void setHolder(String newHolder);
    String getHolder();
    double[] getCoordinates();
}
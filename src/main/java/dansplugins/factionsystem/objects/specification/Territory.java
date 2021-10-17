package dansplugins.factionsystem.objects.specification;

public interface Territory {
    void setHolder(String newHolder);
    String getHolder();
    double[] getCoordinates();
}
package repositories;

import entities.Actualite;
import java.util.List;

public interface IActualiteRepository {
    boolean create(Actualite a);
    List<Actualite> findAllActive();           // voyageur
    List<Actualite> findByAgency(int idAgence);// agency
    boolean archive(int idActualite, int idAgence);
    boolean incrementClick(int idActualite);
    boolean extendEndsAtPlus7Days(int idActualite, int idAgence);
    boolean delete(int idActualite, int idAgence);
}
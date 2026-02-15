package repositories;

import entities.Offre;
import java.util.List;

public interface IOffreRepository {

    List<Offre> findAllAdmin();
    List<Offre> findAllByAgency(int agencyId);

    Offre findById(int idOffre);
    Offre findByIdForAgency(int idOffre, int agencyId);

    int insert(Offre o);

    boolean update(Offre o);
    boolean updateForAgency(Offre o, int agencyId);

    boolean deleteSafe(int idOffre);
    boolean deleteSafeForAgency(int idOffre, int agencyId);

    boolean isUsedInLignePanier(int idOffre);
    boolean isOwnedByAgency(int idOffre, int agencyId);

    List<Offre> findAllAdminByAgency(Integer agencyId); // null => all

}

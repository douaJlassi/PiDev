package repositories;

import entities.AgencyReservationLine;

import java.util.List;

public interface IAgencyReservationsRepository {
    List<AgencyReservationLine> findLinesForAgency(int idAgence);
    boolean approveLine(int idAgence, int idReservation, int idOffre);
    boolean rejectLine(int idAgence, int idReservation, int idOffre, String reason);}
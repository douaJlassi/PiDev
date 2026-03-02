package Controllers;

import entities.Agency;

/**
 * AgencySession — simple singleton that acts as the agency-side session.
 *
 * Set once on successful login, cleared on logout.
 * AgencyDashboardController and any other back-office class reads from here
 * instead of using hardcoded IDs.
 *
 * Usage:
 *   AgencySession.get().setAgency(agency);  // after login
 *   AgencySession.get().getAgency();        // anywhere in back-office
 *   AgencySession.get().clear();            // on logout
 *   AgencySession.get().isLoggedIn();       // guard check
 */
public class AgencySession {

    private static final AgencySession INSTANCE = new AgencySession();
    public static AgencySession get() { return INSTANCE; }
    private AgencySession() {}

    private Agency agency = null;

    public void  setAgency(Agency a) { this.agency = a; }
    public Agency getAgency()        { return agency; }
    public boolean isLoggedIn()      { return agency != null; }
    public void  clear()             { agency = null; }
}
package Services;



public class ActiviteAchatService
{

        private int idAchat;
        private int idActivite;
        private int quantite;
        private double prixUnitaire;

        // Constructeurs
        public ActiviteAchatService() {}

        public ActiviteAchatService(int idAchat, int idActivite, int quantite, double prixUnitaire) {
            this.idAchat = idAchat;
            this.idActivite = idActivite;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
        }

        public int getIdAchat() {
            return idAchat;
        }

        public void setIdAchat(int idAchat) {
            this.idAchat = idAchat;
        }

        public int getIdActivite() {
            return idActivite;
        }

        public void setIdActivite(int idActivite) {
            this.idActivite = idActivite;
        }

        public int getQuantite() {
            return quantite;
        }

        public void setQuantite(int quantite) {
            this.quantite = quantite;
        }

        public double getPrixUnitaire() {
            return prixUnitaire;
        }

        public void setPrixUnitaire(double prixUnitaire) {
            this.prixUnitaire = prixUnitaire;
        }

        @Override
        public String toString() {
            return "AchatActivite{" +
                    "idAchat=" + idAchat +
                    ", idActivite=" + idActivite +
                    ", quantite=" + quantite +
                    ", prixUnitaire=" + prixUnitaire +
                    '}';
        }
    }
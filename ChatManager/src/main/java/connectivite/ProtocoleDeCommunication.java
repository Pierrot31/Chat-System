package connectivite;


import main.ChatManager;
import model.UserLocal;
import model.UsersDistants;

import javax.jws.soap.SOAPBinding;
import java.net.ServerSocket;
import java.util.Set;

public class ProtocoleDeCommunication {
    private ChatManager clavardageManager;

    private boolean dernierSurLaListe = false;

    private TCP_ReceptionMessage tcp_receptionMessage = new TCP_ReceptionMessage();
    private TCP_EnvoieMessage tcp_envoieMessage;

    private UDP_ReceptionMessage udp_receptionMessage = new UDP_ReceptionMessage();
    private UDP_EnvoieMessage udp_envoieMessage;


    private static final String BROADCAST_ADDRESS = "255.255.255.255";


    public ProtocoleDeCommunication(ChatManager theManager) {
        this.clavardageManager = theManager;
    }


    public void ecouteDuReseauEnTCP(int port) {
        try {
            tcp_receptionMessage.listenOnPort(port, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ecouteDuReseauEnUDP(int port) {
        try {
            udp_receptionMessage.listenOnPort(port, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void envoieDunMessageEnTCP(String loginDestinaire, MessageSurLeReseau messageToSend) {
        UsersDistants usersDestinataire = this.clavardageManager.accesALaListeDesUsagers().retourneUnUtilisateurDistantParSonLogin(loginDestinaire);
        String ipAddress = usersDestinataire.getAdresseIP();
        int portDistant = usersDestinataire.getPortDistant();

        tcp_envoieMessage = new TCP_EnvoieMessage();
        try {
            tcp_envoieMessage.sendMessageOn(ipAddress, portDistant, messageToSend);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void envoieDunMessageEnUDP(Set<String> loginDestinaire, MessageSurLeReseau messageToSend) {
        if (!(loginDestinaire.size() == 0)) {
            for (String st : loginDestinaire) {
                UsersDistants usersDestinataire = this.clavardageManager.accesALaListeDesUsagers().retourneUnUtilisateurDistantParSonLogin(st);
                String ipAddress = usersDestinataire.getAdresseIP();
                int portDistant = usersDestinataire.getPortDistant();

                udp_envoieMessage = new UDP_EnvoieMessage();
                try {
                    udp_envoieMessage.sendMessageOn(ipAddress, portDistant, messageToSend);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


    }

    public void onNewIncomingMessage(String messageRecue) {
        String[] messageSurLeReseauRecue = messageRecue.split("[$]", 2);
        Entete enteteDuMessageRentrant = Entete.valueOf(messageSurLeReseauRecue[0]);

        switch (enteteDuMessageRentrant) {
            case ENVOIE_MESSAGE:
                //clavardageManager.envoieAuDessus(messageSurLeReseauRecue[1]);
                String contenuMessageRentrant = messageSurLeReseauRecue[1];
                System.out.println(contenuMessageRentrant);

                break;
            case ENVOIE_USERLOCAL:
                String userDistantAsString = messageSurLeReseauRecue[1];
                String[] attributDesUserDistantAsString = userDistantAsString.split("[,]");
                String loginUserDistant = attributDesUserDistantAsString[0];
                String ipUserDistant = attributDesUserDistantAsString[1];
                String pseudoUserDistant = attributDesUserDistantAsString[2];
                int portDistant = Integer.parseInt(attributDesUserDistantAsString[3]);
                UsersDistants userDistant = new UsersDistants(loginUserDistant, ipUserDistant, pseudoUserDistant, portDistant);
                clavardageManager.accesALaListeDesUsagers().ajouteUnUtilisateurDistantALaListe(userDistant);
                break;

            case DEMANDE_DE_CONNEXION:
                if (dernierSurLaListe){
                    envoieDesUsersDistantAuNouvelEntrant();
                }
                break;

        }

    }

    public void diffusionDuUserLocal() {
        UserLocal userLoc = clavardageManager.returnUserLocal();

        String loginUserLoc = userLoc.useLoginUser();
        String ipAddress = userLoc.useIpUser();
        String pseudoActuel = userLoc.usePseudoUser();
        int port = choixPort();

        UsersDistants userLocalAsDistant = new UsersDistants(loginUserLoc, ipAddress, pseudoActuel, port);


        Set<String> toutLesUtilisateurConnecte = clavardageManager.accesALaListeDesUsagers().retourneToutLesUsagers();
        MessageSurLeReseau messageSurLeReseau = new MessageSurLeReseau(Entete.ENVOIE_USERLOCAL, userLocalAsDistant);


        envoieDunMessageEnUDP(toutLesUtilisateurConnecte, messageSurLeReseau);

    }

    public int choixPort() {


        int localport = 1024;
        boolean bindIsDone = false;

        ServerSocket server = null ;

        int tentative = 0;

        while (!bindIsDone){

            try{

                localport++;
                server = new ServerSocket(localport);
                bindIsDone = true ;
                server.close();
            }catch (Exception e){}
        }

        return localport;
    }


    public void demandeDeConnexion(){
        MessageSurLeReseau demandeDeConnexionMessage = new MessageSurLeReseau(Entete.DEMANDE_DE_CONNEXION, "Vide");
        udp_envoieMessage.sendMessageOn(BROADCAST_ADDRESS, lePort, demandeDeConnexionMessage);

    }
}
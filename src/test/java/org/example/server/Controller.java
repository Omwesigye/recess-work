package org.example.server;

import org.json.*;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Controller {
    JSONObject obj;

    public Controller(JSONObject obj) {
        this.obj = obj;
    }
    private JSONObject login(JSONObject obj) throws SQLException, ClassNotFoundException {
        // logic to log in a student this can work with isAuthenticated == false only (!isAuthenticated)
        Database database = new Database();

        JSONArray tokens = obj.getJSONArray("tokens");
String login = tokens.get(0).toString();
        String username = tokens.get(1).toString();
        String email = tokens.get(2).toString();

        JSONObject clientResponse = new JSONObject();
        clientResponse.put("command", login);
        clientResponse.put("username", username);
        clientResponse.put("email", email);
//checking  participants
        String participantLogin = "SELECT * FROM participant";
        ResultSet participantResultSet = database.read(participantLogin);
        while (participantResultSet.next()) {
            if (username.equals(participantResultSet.getString("username")) && email.equals(participantResultSet.getString("emailAddress"))) {
                // there is a match here

                String regNo = participantResultSet.getString("regNo");

                clientResponse.put("participant_id", participantResultSet.getInt("participant_id"));
                clientResponse.put("regNo", regNo);
                clientResponse.put("schoolName", "undefined");
                clientResponse.put("isStudent", true);
                clientResponse.put("isAuthenticated", true);
                clientResponse.put("status", true);

                return  clientResponse;
            }
        }


        String representativeLogin= "SELECT * FROM school";
        ResultSet representativeResultSet = database.read(representativeLogin);
        while (representativeResultSet.next()) {
            if (username.equals(representativeResultSet.getString("representativeName")) && email.equals(representativeResultSet.getString("representativeEmail"))) {
                // there is a match

                String schoolName = representativeResultSet.getString("name");
                String regNo = representativeResultSet.getString("regNo");

                clientResponse.put("participant_id", 0);
                clientResponse.put("schoolName", schoolName);
                clientResponse.put("regNo", regNo);
                clientResponse.put("isStudent", false);
                clientResponse.put("isAuthenticated", true);
                clientResponse.put("status", true);

                return clientResponse;
            }
        }
//invalid
        clientResponse.put("isStudent", false);
        clientResponse.put("isAuthenticated", false);
        clientResponse.put("status", false);
        clientResponse.put("reason", "Invalid credentials. check the details provided");

        return clientResponse;
    }

    private JSONObject register(JSONObject obj) throws IOException, MessagingException, SQLException, ClassNotFoundException {
        // logic to register student this can work with isAuthenticated == false only (!isAuthenticated)
        Email emailAgent = new Email();
        Database database = new Database();

        JSONArray tokens = obj.getJSONArray("tokens");
        JSONObject participantObj = new JSONObject();
        for (int i = 0; i < 8;i++){
            if (tokens.isNull(i)){
                throw new NullPointerException("token at index"+ i + "is null");
            }
        }
        participantObj.put("register",tokens.get(0));
        participantObj.put("username", tokens.get(1));
        participantObj.put("firstname", tokens.get(2));
        participantObj.put("lastname", tokens.get(3));
        participantObj.put("emailAddress", tokens.get(4));
        participantObj.put("dob", tokens.get(5));
        participantObj.put("regNo", tokens.get(6));
        participantObj.put("imagePath", tokens.get(7));

        JSONObject clientResponse = new JSONObject();
        clientResponse.put("command", "register");

        ResultSet rs = database.getRepresentative(participantObj.getString("regNo"));
        String representativeEmail;
        if (rs.next()) {
            representativeEmail = rs.getString("representativeEmail");
        } else {
            clientResponse.put("status", false);
            clientResponse.put("reason", "school unavailable in our database");

            return clientResponse;
        }


        FileStorage fileStorage = new FileStorage("participants.json");
        if (!fileStorage.read().toString().contains(participantObj.toString())) {
            fileStorage.add(participantObj);
            clientResponse.put("status", true);
            clientResponse.put("reason", "Participant added successfully wait for approval");

            emailAgent.sendParticipantRegistrationRequestEmail(representativeEmail, participantObj.getString("emailAddress"), participantObj.getString("username"));

            return clientResponse;
        }

        clientResponse.put("status", false);
        clientResponse.put("reason", " participant object already exists");

        return clientResponse;
    }

    private JSONObject attemptChallenge(JSONObject obj) throws SQLException, ClassNotFoundException {
        // logic to attempt a challenge respond with the random questions if user is eligible (student, isAuthenticated)
        JSONObject clientResponse = new JSONObject();
        JSONArray questions = new JSONArray();

        Database database = new Database();


        int challengeId = Integer.parseInt((String) new JSONArray(obj.get("tokens").toString()).get(1));
        ResultSet challengeQuestions;
        challengeQuestions = database.getChallengeQuestions(challengeId);


        while (challengeQuestions.next()) {
            JSONObject question = new JSONObject();
            question.put("id", challengeQuestions.getString("question_id"));
            question.put("question", challengeQuestions.getString("question"));
            question.put("score", challengeQuestions.getString("score"));

            questions.put(question);
        }

        clientResponse.put("command", "attemptChallenge");
        clientResponse.put("questions", questions);
        clientResponse.put("challenge_id", challengeId);
        clientResponse.put("challenge_name", challengeId);
        return clientResponse;
    }

    private JSONObject viewChallenges(JSONObject obj) throws SQLException, ClassNotFoundException {
        JSONObject clientResponse = new JSONObject();

        Database database = new Database();
        ResultSet availableChallenges = database.getChallenges();
        JSONArray challenges = new JSONArray();

        while (availableChallenges.next()) {
            JSONObject challenge = new JSONObject();
            challenge.put("id", availableChallenges.getInt("challenge_id"));
            challenge.put("name", availableChallenges.getString("challenge_name"));
            challenge.put("difficulty", availableChallenges.getString("difficulty"));
            challenge.put("time_allocation", availableChallenges.getInt("time_allocation"));
            challenge.put("starting_date", availableChallenges.getDate("starting_date"));
            challenge.put("closing_date", availableChallenges.getDate("closing_date"));

            challenges.put(challenge);
        }

        clientResponse.put("command", "viewChallenges");
        clientResponse.put("challenges", challenges.toString());

        return clientResponse;
    }

    private JSONObject confirm(JSONObject obj) throws IOException, SQLException, ClassNotFoundException {
        // logic to confirm registered students (representatives, isAuthenticated)
        FileStorage fileStorage = new FileStorage("participants.json");

        String username = obj.getString("username");
        JSONObject participant = fileStorage.readEntryByUserName(username);
        JSONObject clientResponse = new JSONObject();
        clientResponse.put("command", "confirm");

        if (participant.isEmpty()) {
            clientResponse.put("status", false);
            clientResponse.put("reason", "Invalid command check the username provided");
            return clientResponse;
        }

        Database database = new Database();
        if (obj.getBoolean("confirm")) {
            database.createParticipant(participant.getString("username"), participant.getString("firstname"), participant.getString("lastname"), participant.getString("emailAddress"), participant.getString("dob"), participant.getString("regNo"), participant.getString("imagePath"));
            fileStorage.deleteEntryByUserName(username);
            clientResponse.put("reason", participant.getString("firstname") + " " + participant.getString("firstname") + " " + participant.getString("emailAddress") + " confirmed successfully");
        } else {
            database.createParticipantRejected(participant.getString("username"), participant.getString("firstname"), participant.getString("lastname"), participant.getString("emailAddress"), participant.getString("dob"), participant.getString("regNo"), participant.getString("imagePath"));
            fileStorage.deleteEntryByUserName(username);
            clientResponse.put("reason", participant.getString("firstname") + " " + participant.getString("firstname") + " " + participant.getString("emailAddress") + " rejected successfully");
        }
        clientResponse.put("status", true);
        return clientResponse;
    }

    private JSONObject viewApplicants(JSONObject obj) throws IOException {
        // logic to view applicants
        String regNo = obj.getString("regNo");

        FileStorage fileStorage = new FileStorage("participants.json");

        String participants = fileStorage.filterParticipantsByRegNo(regNo);

        JSONObject clientResponse = new JSONObject();
        clientResponse.put("command", "viewApplicants");
        clientResponse.put("applicants", participants);


        return clientResponse;
    }

    public JSONObject attempt(JSONObject obj) throws SQLException, ClassNotFoundException {
        JSONArray attempt = obj.getJSONArray("attempt");
        Database database = new Database();

        JSONObject attemptEvaluation = new JSONObject();
        attemptEvaluation.put("score", database.getAttemptScore(attempt));
        attemptEvaluation.put("participant_id", obj.getInt("participant_id"));
        attemptEvaluation.put("challenge_id", obj.getInt("challenge_id"));
        attemptEvaluation.put("total_score", obj.getInt("total_score"));

        database.createChallengeAttempt(attemptEvaluation);

        return new JSONObject();
    }

    public JSONObject run() throws IOException, SQLException, ClassNotFoundException, MessagingException {
        switch (this.obj.get("command").toString()) {
            case "login":
                return this.login(this.obj);

            case "register":
                return this.register(this.obj);

            case "viewChallenges":
                return this.viewChallenges(this.obj);

            case "attemptChallenge":

                return this.attemptChallenge(this.obj);

            case "confirm":

                return this.confirm(this.obj);

            case "viewApplicants":
                return this.viewApplicants(this.obj);

            case "attempt":
                return this.attempt(this.obj);

            default:
                // command unresolved
                JSONObject outputObj = new JSONObject();
                outputObj.put("command", "exception");
                outputObj.put("reason", "Invalid command");

                return outputObj;
 }
    }
}

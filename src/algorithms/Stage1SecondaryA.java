/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BootingBerzerk.java 2014-11-03 buixuan.
 * ******************************************************/
package algorithms;

import java.util.ArrayList;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

public class Stage1SecondaryA extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;

  //---VARIABLES---//
  private boolean turnTask,turnRight,moveTask,berzerk,back;
  private double endTaskDirection,lastSeenDirection;
  private int endTaskCounter,berzerkInerty;
  private boolean firstMove,berzerkTurning;
  private ArrayList<Double> positionReceived;
  private int state;
  private double oldAngle;
  private double myX,myY;
  private boolean isMoving;
  private boolean canHelpTeammate;
  private int whoAmI;
  private int msgReadingIndex;
  private double teammateX, teammateY;
  
  
  
  //---CONSTRUCTORS---//
  public Stage1SecondaryA() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
	positionReceived = new ArrayList<Double>();
	msgReadingIndex = 0;
	canHelpTeammate = false;
    turnTask=true;
    moveTask=false;
    firstMove=true;
    berzerk=false;
    berzerkInerty=0;
    berzerkTurning=false;
    back=false;
    endTaskDirection=(Math.random()-0.5)*0.5*Math.PI;
    turnRight=(endTaskDirection>0);
    endTaskDirection+=getHeading();
    lastSeenDirection=Math.random()*Math.PI*2;
    if (turnRight) stepTurn(Parameters.Direction.RIGHT);
    else stepTurn(Parameters.Direction.LEFT);
    sendLogMessage("Turning point. Waza!");
  }
  public void step() {
    /*if (Math.random()<0.01 && !berzerk) {
      fire(Math.random()*Math.PI*2);
      return;
    }*/
    ArrayList<IRadarResult> radarResults = detectRadar();
    fire(2.0);
    
    if (isMoving){
        myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
        myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
        isMoving=false;
    }
    
    if (  !detectRadar().isEmpty() ) {
     
   
    
    if ( detectFront().getObjectType()==IFrontSensorResult.Types.WALL && detectRadar().get(0).getObjectDistance() < 10 ) {
        isMoving = false;
        sendLogMessage("OBSTACLE");
        return;
        //detect radar -> list
    }
    }
    sendLogMessage("I think I'm rolling at position ("+(int)myX+", "+(int)myY+").");
      
    
    
    if (berzerk) {
      if (berzerkTurning) {
        endTaskCounter--;
        if (isHeading(endTaskDirection)) {
          berzerkTurning=false;
          move();
          isMoving = true;
        } else {
        	isMoving = false;
          if (turnRight) stepTurn(Parameters.Direction.RIGHT);
          else stepTurn(Parameters.Direction.LEFT);
        }
        return;
      }
      /*if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        berzerk=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      } else {
        endTaskCounter--;
        if (Math.random()<0.1) {
          for (IRadarResult r : radarResults) {
            if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
              fire(r.getObjectDirection());
              lastSeenDirection=r.getObjectDirection();
              return;
            }
          }
          fire(lastSeenDirection);
          return;
        } else {
          if (back) moveBack(); else move();
        }
      }*/
      if (berzerkInerty>50) {
    	  isMoving = false;
        turnTask=true;
        moveTask=false;
        berzerk=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        return;
      }
      if (endTaskCounter<0) {
    	  isMoving = false;
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            fire(r.getObjectDirection());
            lastSeenDirection=r.getObjectDirection();
            berzerkInerty=0;
            return;
          }
        }
        fire(lastSeenDirection);
        berzerkInerty++;
        endTaskCounter=21;
        return;
      } else {
        endTaskCounter--;
        for (IRadarResult r : radarResults) {
          if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
            lastSeenDirection=r.getObjectDirection();
            berzerkInerty=0;
            move();
            isMoving = true;
            return;
          }
        }
        berzerkInerty++;
        move();
        isMoving = true;
      }
      return;
    }
    if (radarResults.size()!=0){
    	isMoving = false;
    	
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentMainBot) {
          berzerk=true;
          back=(Math.cos(getHeading()-r.getObjectDirection())>0);
          endTaskCounter=21;
          fire(r.getObjectDirection());
          lastSeenDirection=r.getObjectDirection();
          berzerkTurning=true;
          endTaskDirection=lastSeenDirection;
          double ref=endTaskDirection-getHeading();
          if (ref<0) ref+=Math.PI*2;
          turnRight=(ref>0 && ref<Math.PI);
          return;
        }
      }
      for (IRadarResult r : radarResults) {
        if (r.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
        	canHelpTeammate = false;
          //ennemi détecté 
          broadcast(myX + " " + myY); //appel de renforts
    	  sendLogMessage("sending message for team mates - ennemy detected !");
          fire(r.getObjectDirection());
          return;
        }
      }
      canHelpTeammate = true;
      //si aucun ennemi detecte, le robot est libre et peut aider un coéquipier si besoin
      if(canHelpTeammate) {
    	  ArrayList<String> receivedMessage = fetchAllMessages();
    	  if(!receivedMessage.isEmpty()) {
    	  String positionToGo = receivedMessage.get(msgReadingIndex);
    	  String coordinates[] = positionToGo.split(" ");
    	  teammateX = Double.parseDouble(coordinates[0]);
    	  teammateY = Double.parseDouble(coordinates[1]);
    	  sendLogMessage("received message from team mates - ennemy detected !");
    	  if(myX < teammateX) {
    		  //go east
    		  stepTurn(Parameters.Direction.LEFT);
 			 move();
    		  return;
    	  }
		 if(myX > teammateX) {
		      //go west 
			 stepTurn(Parameters.Direction.RIGHT);
			 move();
			 return;
		    	  }
		 if(myY < teammateY) {
			  // go south
			 stepTurn(Parameters.Direction.LEFT);
			 move();
			 return;
		 }
		 if(myY > teammateY) {
			  // go north
			 stepTurn(Parameters.Direction.LEFT);
			 move();
			 return;

		 }
    	  //ne lis le message suivant que si l'ennemi designé par le message actuel est tué
      }
      }
      
      
    }
    if (turnTask) {
      if (isHeading(endTaskDirection)) {
        if (firstMove) {
          firstMove=false;
  	  turnTask=false;
          moveTask=true;
          endTaskCounter=400;
          isMoving = true;
	  move();
          return;
        }
	turnTask=false;
        moveTask=true;
        endTaskCounter=100;
        isMoving = true;
	move();
        sendLogMessage("Moving a head. Waza!");
      } else {
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
      }
      isMoving = false;
      return;
    }
    if (moveTask) {
      /*if (detectFront()!=NOTHING) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
        sendLogMessage("Turning point. Waza!");
      }*/
      if (endTaskCounter<0) {
        turnTask=true;
        moveTask=false;
        endTaskDirection=(Math.random()-0.5)*2*Math.PI;
        turnRight=(endTaskDirection>0);
        endTaskDirection+=getHeading();
        if (turnRight) stepTurn(Parameters.Direction.RIGHT);
        else stepTurn(Parameters.Direction.LEFT);
      } else {
        endTaskCounter--;
        isMoving = true;
        move();
      }
      return;
    }
    return;
  }

  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamAMainBotStepTurnAngle;
  }
}
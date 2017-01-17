package server;

import co.paralleluniverse.actors.ActorRef;
import co.paralleluniverse.actors.BasicActor;
import co.paralleluniverse.fibers.SuspendExecution;
import server.Message.Type;
import java.util.HashMap;

/**
 * Manages Chatroom-related requests, and sends relevant notifications to a publisher.
 */
public class ChatRoomManager extends BasicActor<Message, Void> {

    private final HashMap<String,ActorRef> chatroomList;
    private final ActorRef publisher;
    
    public ChatRoomManager(ActorRef publisher){
        this.chatroomList = new HashMap<>();
        this.publisher = publisher;
    }
    
    
    // TEMPORARY INIT
    private void init(){
        ChatRoom chatroom = new ChatRoom("Room1000", publisher);
        chatroomList.put("Room1000", chatroom.spawn());
    }
    
    @Override
    @SuppressWarnings("empty-statement")
    protected Void doRun() throws InterruptedException, SuspendExecution {
        init();
            while(receive(message -> {
                
                RoomRequest rqst;
                ActorRef user;
                String info;
                switch(message.type){
                    
                    case JOIN:
                        rqst = (RoomRequest) message.o;
                        RoomRequest reply = new RoomRequest(rqst.getActor(),rqst.getRoomName(),rqst.getUsrname());
                        if(chatroomList.containsKey(rqst.getRoomName())){
                            ActorRef room = chatroomList.get(rqst.getRoomName());
                            room.send(new Message(Type.ENTER, rqst));
                            reply.setActor(room);
                            rqst.getActor().send(new Message(Type.JOIN_OK, reply));
                        }
                        else
                            rqst.getActor().send(new Message(Type.ERROR_RDE, message.o));
                        return true;
                        
                    case LIST_ROOM:
                        ActorRef actor = (ActorRef) message.o;
                        String list = printRoomList();
                        if(list != null)
                            actor.send(new Message(Type.LIST, list));
                        else{
                            list = "There are no rooms available";
                            actor.send(new Message(Type.LIST, list));
                        }   
                        return true;
                        
                    case CREATE_ROOM:
                        rqst = (RoomRequest) message.o;
                        info = "info:croom: Room " + rqst.getRoomName() + " has been created";
                        
                        if(!chatroomList.containsKey(rqst.getRoomName())){
                            ActorRef newchatroom = new ChatRoom(rqst.getRoomName(),publisher).spawn();
                            chatroomList.put(rqst.getRoomName(), newchatroom);
                            rqst.getActor().send(new Message(Type.CRTRM_OK, message.o));
                            publisher.send(new Message(null,info));
                        }
                        else
                            rqst.getActor().send(new Message(Type.ERROR_RAE, message.o));
                        
                        return true;
                        
                    case DELETE_ROOM:
                        rqst = (RoomRequest) message.o;
                        info = "info:rroom: Room " + rqst.getRoomName() + " has been deleted";
                        
                        if(chatroomList.containsKey(rqst.getRoomName())){
                            ActorRef room = chatroomList.get(rqst.getRoomName());
                            room.send(new Message(Type.DESTROY, null));
                            chatroomList.remove(rqst.getRoomName());
                            rqst.getActor().send(new Message(Type.DELRM_OK, message.o));
                            
                            publisher.send(new Message(null,info));
                        }
                        else
                            rqst.getActor().send(new Message(Type.ERROR_RDE, message.o));
                        
                        return true;
                }
            
                return null;
            }));
            
            
            return null;
    }
    
    public String printRoomList(){
        StringBuilder sb = new StringBuilder();
        sb.append("==========================\nRoom List:\n");
        for(String name : chatroomList.keySet())
            sb.append(name).append("\n");
        sb.append("==========================\n\n");
        return sb.toString();
    }
   
}

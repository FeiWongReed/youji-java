package org.ritsuka.youji;

import akka.actor.ActorRef;
import akka.actor.Actors;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import org.ritsuka.youji.event.AppShutdownEvent;
import org.ritsuka.youji.event.RunXmppWorkerEvent;
import org.ritsuka.youji.util.Log;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import static akka.actor.Actors.actorOf;

/**
 * Date: 9/29/11
 * Time: 8:48 PM
 */
public class Supervisor extends UntypedActor {
    final Log log = new Log(LoggerFactory.getLogger("SV"));
    private Log log() {
        return this.log;
    }

    private final CountDownLatch latch;

    ArrayList<ActorRef> workers = new ArrayList<ActorRef>();

    static public UntypedActorFactory create(final CountDownLatch latch) {
        return new UntypedActorFactory() {
            public UntypedActor create() {
                return new Supervisor(latch);
            }
        };
    }
    private Supervisor(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        log().debug("Message: {}", message);

        if (message instanceof AccountData) {
            ActorRef worker = actorOf(XMPPWorker.create((AccountData)message)).start();
            workers.add(worker);
            worker.tell(new RunXmppWorkerEvent(), getContext());
        } else if (message instanceof AppShutdownEvent)
        {
            ActorRef[] workers = Actors.registry().actorsFor(XMPPWorker.class);
            for (ActorRef worker:workers)
                worker.stop();
            log().debug("Supervisor ready to shutdown");
            ((ActorRef)self()).stop();
        }
        else
            throw new IllegalArgumentException("Unknown message [" + message + "]");
    }

    @Override
    public void postStop() {
        log().debug("supervisor ended");
        latch.countDown();
    }

    public static ActorRef instance() {
        ActorRef[] supervisors = Actors.registry().actorsFor(Supervisor.class);
        assert 1 == supervisors.length;
        for (ActorRef supervisor:supervisors)
            return supervisor;
        return null;
    }
}

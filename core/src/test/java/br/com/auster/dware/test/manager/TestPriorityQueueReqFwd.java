/*
 * Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Created on Apr 8, 2005
 */
package br.com.auster.dware.test.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.DataAwareManagerMediator;
import br.com.auster.dware.manager.GraphGroup;
import br.com.auster.dware.manager.GraphManager;
import br.com.auster.dware.manager.PriorityQueueReqForwarder;
import br.com.auster.dware.manager.ReqForwarderInterface;
import br.com.auster.dware.manager.checkpoint.AbstractCheckpoint;
import br.com.auster.dware.monitor.manager.JMXGraphGroupCounter;

/**
 * This class handles all unit tests over class <code>
 * br.com.auster.dware.manager.PriorityQueueReqForwarder</code>.
 * 
 * @version $Id: TestPriorityQueueReqFwd.java 358 2008-03-10 23:55:46Z lmorozow $
 */
public class TestPriorityQueueReqFwd extends TestCase {

  /**
   * Constructor
   * 
   * @param method
   *          test method name.
   */
  public TestPriorityQueueReqFwd(String method) {
    super(method);
  }

  protected void setUp() {
    // force classloader to loads PriorityQueueReqForwarder class... otherwise
    // I18n crashes.
    try {
    	Class.forName("br.com.auster.dware.manager.PriorityQueueReqForwarder");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Tests if NoSuchElementException is thrown when the priorityqueue is empty
   */
  public void testNoSuchElement() {
    DataAwareManagerMediator mediator = new DummyDataAwareManagerMediator();
	PriorityQueueReqForwarder pqrf = new PriorityQueueReqForwarder(mediator);
    boolean ok = false;
    try {
      pqrf.chooseNextRequest(30);
    } catch (NoSuchElementException ne) {
      ok = true;
    }
    if (!ok)
      Assert.fail("Should throw NoSuchElementException...");

    ok = false;
    pqrf.addNewReq(new TestRequest(1));
    try {
      pqrf.chooseNextRequest(30);
    } catch (NoSuchElementException ne) {
      Assert.fail("The priority queue is not empty it shoud return some request...");
    }
    try {
      pqrf.chooseNextRequest(30);
    } catch (NoSuchElementException ne) {
      ok = true;
    }
    if (!ok)
      Assert.fail("Should throw NoSuchElementException...");
  }

  /**
   * Tests addNewReq and chooseNextRequest.
   */
  public void testAddGetRequests() {
    DataAwareManagerMediator mediator = new DummyDataAwareManagerMediator();
	PriorityQueueReqForwarder pqrf = new PriorityQueueReqForwarder(mediator);
    int[] weights = { 10, 5, 3, 19, 2 };
    for (int i = 0; i < weights.length; i++) {
      pqrf.addNewReq(new TestRequest(weights[i]));
      try {
        Assert.assertEquals((pqrf.chooseNextRequest(30)).getWeight(), weights[i]);
      } catch (NoSuchElementException ne) {
        Assert.fail(ne.getMessage());
      }
    }
  }

  /**
   * Verifies if all requests are returned.
   */
  public void testAllReqReturned() {
    DataAwareManagerMediator mediator = new DummyDataAwareManagerMediator();
	PriorityQueueReqForwarder pqrf = new PriorityQueueReqForwarder(mediator);
    int[] weights = { 10, 5, 3, 19, 2 };
    HashSet reqs = new HashSet();
    // put all
    for (int i = 0; i < weights.length; i++) {
      TestRequest tr = new TestRequest(weights[i]);
      reqs.add(tr);
      pqrf.addNewReq(tr);
    }
    try {
      // get all
      for (int i = 0; i < weights.length; i++) {
        TestRequest tr = (TestRequest) pqrf.chooseNextRequest(30);
        if (reqs.contains(tr))
          reqs.remove(tr);
      }
    } catch (NoSuchElementException ne) {
      Assert.fail(ne.getMessage());
    }

    // verifies if there is some request that wasn't got.
    Assert.assertEquals(reqs.size(), 0);
  }

  /**
   * Verifies if the request returned is with weight expected.
   */
  public void testWeightRequested() {
    DataAwareManagerMediator mediator = new DummyDataAwareManagerMediator();
	PriorityQueueReqForwarder pqrf = new PriorityQueueReqForwarder(mediator);
    int[] weights = { 10, 5, 3, 19, 2, 34, 22, 9, 11, 29, 16, 30, 13, 7, 21, 27 };
    ArrayList reqs = new ArrayList();
    // put all
    for (int i = 0; i < weights.length; i++) {
      TestRequest tr = new TestRequest(weights[i]);
      reqs.add(tr);
      pqrf.addNewReq(tr);
    }

    try {
      // get all
      while (reqs.size() > 0) {
        long wishWei = pqrf.getWishWeight();
        TestRequest tr = (TestRequest) pqrf.chooseNextRequest(wishWei);// ((Integer)weiList.get(i)).intValue()+1);
        if (reqs.contains(tr))
          reqs.remove(reqs.indexOf(tr));
        else
          Assert.fail("Could not get a request expected!");
      }
    } catch (NoSuchElementException ne) {
      Assert.fail(ne.getMessage());
    }
  }

  /**
   * Multi thread test for getting requests from priority queue.
   */
  public void testMultiThreadGetRequests() throws Throwable {

    DataAwareManagerMediator mediator = new DummyDataAwareManagerMediator();
	PriorityQueueReqForwarder pqrf = new PriorityQueueReqForwarder(mediator);
    int maxReq = 3000;
    ArrayList reqs = new ArrayList();

    // put all
    for (int i = 0; i < maxReq; i++) {
      TestRequest tr = new TestRequest(i);
      reqs.add(tr);
      pqrf.addNewReq(tr);
    }

    // create fake graph groups
    int numberGG = 5;
    SimulatedGraphGroup[] trs = new SimulatedGraphGroup[numberGG];
    // instantiate the TestRunnable classes
    for (int i = 0; i < numberGG; i++)
      trs[i] = new SimulatedGraphGroup("gg" + i, pqrf, numberGG * maxReq);

    // pass that instance to the MTTR
    MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

    // kickstarts the MTTR & fires off threads
    mttr.runTestRunnables();

    // after all thread are finished their requests should be the ones
    // before in PriorityQueueReqForwarder object.

    // concat the request results
    ArrayList ggReqs = new ArrayList();
    for (int i = 0; i < numberGG; i++)
      ggReqs.addAll(trs[i].getReqs());

    // the size must be equals
    Assert.assertEquals(reqs.size(), ggReqs.size());

    for (int i = 0; i < maxReq; i++) {
      if (!ggReqs.contains(reqs.get(i)))
        Assert.fail("Some request that was on priority queue is not in any fake graph groups.");
      else
        ggReqs.remove(reqs.get(i));
    }
  }

  /**
   * Multi thread test for priority queue adding requests concurrently.
   */
  public void testMultiThreadAddRequests() throws Throwable {

    DataAwareManagerMediator mediator = new DummyDataAwareManagerMediator();
	PriorityQueueReqForwarder pqrf = new PriorityQueueReqForwarder(mediator);
    ArrayList reqs = new ArrayList();

    // create request creators
    int numberGG = 5;
    int maxReqCreated = 5000;
    Random rand = new Random(numberGG);
    int ini = 0, end = 0;
    TestReqFeeder[] trs = new TestReqFeeder[numberGG];
    // instantiate the TestRunnable classes
    for (int i = 0; i < numberGG; i++) {
      end += (long) (maxReqCreated * rand.nextFloat());
      trs[i] = new TestReqFeeder("gg" + i, pqrf, ini, end);
      ini = end;
    }

    // pass that instance to the MTTR
    MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

    // kickstarts the MTTR & fires off threads
    mttr.runTestRunnables();

    // after all thread are finished their requests should be the ones
    // before in PriorityQueueReqForwarder object.

    // concat the request created
    ArrayList ggReqs = new ArrayList();
    for (int i = 0; i < numberGG; i++)
      ggReqs.addAll(trs[i].getReqs());

    // the size must be equals
    Assert.assertEquals(end, ggReqs.size());

    try {
      for (int i = 0; i < end; i++) {
        TestRequest tr = (TestRequest) pqrf.chooseNextRequest(end);
        if (!ggReqs.contains(tr))
          Assert.fail("Unexpected request found on concurrent adding requests test.");
        else
          ggReqs.remove(tr);
      }
    } catch (NoSuchElementException ne) {
      Assert.fail("Requests added are missing on concurrent adding requests test.");
    }
    // the size must be equals
    Assert.assertEquals(0, ggReqs.size());
  }

  /**
   * Multi thread test for priority queue adding and getting requests
   * concurrently.
   */
  public void testMultiThreadAddGetConcurrentRequests() throws Throwable {

    DataAwareManagerMediator mediator = new DummyDataAwareManagerMediator();
	PriorityQueueReqForwarder pqrf = new PriorityQueueReqForwarder(mediator);
    ArrayList reqs = new ArrayList();

    // create request creators
    int numberRF = 3;
    int numberGG = 5;
    int maxReqCreated = 5000;
    Random rand = new Random(numberGG);
    int ini = 0, end = 0;
    TestRunnable[] trs = new TestRunnable[numberGG + numberRF];
    // instantiate the TestRunnable classes
    for (int i = 0; i < numberRF; i++) {
      end += (long) (maxReqCreated * rand.nextFloat());
      trs[i] = new TestReqFeeder("gg" + i, pqrf, ini, end);
      ini = end;
    }
    // instantiate the TestRunnable classes
    for (int i = numberRF; i < numberGG + numberRF; i++)
      trs[i] = new SimulatedGraphGroup("gg" + i, pqrf, end);

    // pass that instance to the MTTR
    MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

    // kickstarts the MTTR & fires off threads
    mttr.runTestRunnables();

    // after all thread are finished their requests should be the ones
    // before in PriorityQueueReqForwarder object.

    // concat the request created
    ArrayList rfReqs = new ArrayList();
    for (int i = 0; i < numberRF; i++)
      rfReqs.addAll(((TestReqFeeder) trs[i]).getReqs());
    ArrayList ggReqs = new ArrayList();
    for (int i = numberRF; i < numberGG + numberRF; i++)
      ggReqs.addAll(((SimulatedGraphGroup) trs[i]).getReqs());

    // the size must be equals
    Assert.assertEquals(end, ggReqs.size());
    Assert.assertEquals(end, rfReqs.size());

    for (int i = 0; i < end; i++) {
      TestRequest tr = (TestRequest) rfReqs.get(i);
      if (!ggReqs.contains(tr))
        Assert
            .fail("A request created was not found on ant graph group on concurrent add/get requests test.");
      else
        ggReqs.remove(tr);
    }
  }

  /*
   * Helper request
   */
  class TestRequest extends Request {

    private int wei;

    public TestRequest(int wei) {
      this.wei = wei;
      setUserKey(Integer.toString(wei));
    }

    public long getWeight() {
      return wei;
    }

    public int hashCode() {
      return wei;
    }

    public String toString() {
      return Integer.toString(wei);
    }
  }

  /*
   * Simulates a graph group, it extends
   * net.sourceforge.groboutils.junit.v1.TestRunnable to allows multi thread
   * testing.
   */
  class SimulatedGraphGroup extends TestRunnable {

    private PriorityQueueReqForwarder pqforw;

    private long maxWei;

    private ArrayList myRequests;

    private String name;

    public SimulatedGraphGroup(String _name, PriorityQueueReqForwarder _pqforw, long _maxWei) {
      name = _name;
      pqforw = _pqforw;
      maxWei = _maxWei;
      myRequests = new ArrayList();
    }

    public void runTest() throws Throwable {

      Random rand = new Random(maxWei);
      boolean finish = false;

      while (!finish) {
        try {
          Thread.sleep(10); // simulates processement
          TestRequest req = (TestRequest) pqforw.chooseNextRequest((long) (maxWei * rand
              .nextFloat()));
          // System.out.println(name+" : adding "+req.hashCode());
          myRequests.add(req);
        } catch (NoSuchElementException ne) {
          finish = true;
        }
      }
    }

    public ArrayList getReqs() {
      return myRequests;
    }
  }

  /*
   * Adds requests to priority queue.
   */
  class TestReqFeeder extends TestRunnable {

    private PriorityQueueReqForwarder pqforw;

    private ArrayList myRequests;

    private String name;

    private int iniCode, endCode;

    public TestReqFeeder(String _name, PriorityQueueReqForwarder _pqforw, int _iniCode, int _endCode) {
      name = _name;
      pqforw = _pqforw;
      iniCode = _iniCode;
      endCode = _endCode;
      myRequests = new ArrayList();
    }

    public void runTest() throws Throwable {

      Random rand = new Random(iniCode);

      for (int i = iniCode; i < endCode; i++) {
        TestRequest tr = new TestRequest(i);
        myRequests.add(tr);
        pqforw.addNewReq(tr);
        Thread.sleep((long) (10 * rand.nextFloat()));
      }
    }

    public ArrayList getReqs() {
      return myRequests;
    }
  }

  class DummyDataAwareManagerMediator implements DataAwareManagerMediator {

	public boolean checkIfReqLoaded(Request req) {
		return false;
	}

	public JMXGraphGroupCounter getJMXCounters() {
		return null;
	}

	public void registerCheckpoint(AbstractCheckpoint pt) {
		// nothing to do
	}

	public void registerGraphGroup(GraphGroup graphGp) {
		// nothing to do
	}

	public void registerGraphManager(GraphManager gm) {
		// nothing to do
	}

	public void registerReqForwarder(ReqForwarderInterface fwd) {
		// nothing to do
	}

	public void reqFailed(Request req, String graphName, Throwable error) {
		// nothing to do
	}

	public void reqProcessed(Request req) {
		// nothing to do
	}

	public void reqQueued(Request req) {
		// nothing to do
	}

	public void reqRequeued(Request req) {
		// nothing to do
	}

	public void shutdown() {
		// nothing to do
	}

	public void unregisterGraphGroup(String graphGpName) {
		// nothing to do
	}
  }
}

package org.jaybaws.metrics.bw;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class BW5MicrometerAgentTest
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    @Test
    public void myStringCompareTest() {
        String a = "com.tibco.bw:key=engine,name=\"PortSmokeTester-PortSmokeTester_LB\"";
        assertTrue(a.startsWith("com.tibco.bw:key=engine"));
    }

    @Test
    public void myStringLengthTest() {
        String a = "com.tibco.bw";
        assertTrue(a.length() == 12);
    }

}

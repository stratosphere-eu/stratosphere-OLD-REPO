package eu.stratosphere.pact.test.failingPrograms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import eu.stratosphere.nephele.configuration.Configuration;
import eu.stratosphere.nephele.jobgraph.JobGraph;
import eu.stratosphere.pact.common.contract.DataSinkContract;
import eu.stratosphere.pact.common.contract.DataSourceContract;
import eu.stratosphere.pact.common.contract.MapContract;
import eu.stratosphere.pact.common.io.TextInputFormat;
import eu.stratosphere.pact.common.io.TextOutputFormat;
import eu.stratosphere.pact.common.plan.Plan;
import eu.stratosphere.pact.common.stub.Collector;
import eu.stratosphere.pact.common.stub.MapStub;
import eu.stratosphere.pact.common.type.KeyValuePair;
import eu.stratosphere.pact.common.type.base.PactInteger;
import eu.stratosphere.pact.common.type.base.PactString;
import eu.stratosphere.pact.compiler.PactCompiler;
import eu.stratosphere.pact.compiler.jobgen.JobGraphGenerator;
import eu.stratosphere.pact.compiler.plan.OptimizedPlan;
import eu.stratosphere.pact.test.util.FailingTestBase;

/**
 * Tests whether the system recovers from a runtime exception from the PACT user code.
 * 
 * @author Fabian Hueske (fabian.hueske@tu-berlin.de)
 *
 */
@RunWith(Parameterized.class)
public class RuntimeExceptionFailureITCase extends FailingTestBase {

	/**
	 * {@inheritDoc}
	 * 
	 * @param clusterConfig
	 * @param testConfig
	 */
	public RuntimeExceptionFailureITCase(String clusterConfig, Configuration testConfig) {
		super(testConfig,clusterConfig);
	}

	// log
	private static final Log LOG = LogFactory.getLog(RuntimeExceptionFailureITCase.class);

	// input for map tasks
	private static final String MAP_IN_1 = "1 1\n2 2\n2 8\n4 4\n4 4\n6 6\n7 7\n8 8\n";
	private static final String MAP_IN_2 = "1 1\n2 2\n2 2\n4 4\n4 4\n6 3\n5 9\n8 8\n";
	private static final String MAP_IN_3 = "1 1\n2 2\n2 2\n3 0\n4 4\n5 9\n7 7\n8 8\n";
	private static final String MAP_IN_4 = "1 1\n9 1\n5 9\n4 4\n4 4\n6 6\n7 7\n8 8\n";

	// expected result of working map job
	private static final String MAP_RESULT = "1 11\n2 12\n4 14\n4 14\n1 11\n2 12\n2 12\n4 14\n4 14\n3 16\n1 11\n2 12\n2 12\n0 13\n4 14\n1 11\n4 14\n4 14\n";

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void preSubmit() throws Exception {
		String tempDir = getFilesystemProvider().getTempDirPath();
		
		getFilesystemProvider().createDir(tempDir + "/mapInput");
		
		getFilesystemProvider().createFile(tempDir+"/mapInput/mapTest_1.txt", MAP_IN_1);
		getFilesystemProvider().createFile(tempDir+"/mapInput/mapTest_2.txt", MAP_IN_2);
		getFilesystemProvider().createFile(tempDir+"/mapInput/mapTest_3.txt", MAP_IN_3);
		getFilesystemProvider().createFile(tempDir+"/mapInput/mapTest_4.txt", MAP_IN_4);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JobGraph getFailingJobGraph() throws Exception {

		// path prefix for temp data 
		String pathPrefix = getFilesystemProvider().getURIPrefix()+getFilesystemProvider().getTempDirPath();
		
		// init data source 
		DataSourceContract<PactString, PactString> input = new DataSourceContract<PactString, PactString>(
			MapTestInFormat.class, pathPrefix+"/mapInput");
		input.setFormatParameter("delimiter", "\n");
		input.setDegreeOfParallelism(config.getInteger("MapTest#NoSubtasks", 1));

		// init failing map task
		MapContract<PactString, PactString, PactString, PactInteger> testMapper = new MapContract<PactString, PactString, PactString, PactInteger>(FailingMapper.class);
		testMapper.setDegreeOfParallelism(config.getInteger("MapTest#NoSubtasks", 1));

		// init data sink
		DataSinkContract<PactString, PactInteger> output = new DataSinkContract<PactString, PactInteger>(
			MapTestOutFormat.class, pathPrefix + "/result.txt");
		output.setDegreeOfParallelism(1);

		// compose failing program
		output.setInput(testMapper);
		testMapper.setInput(input);

		// generate plan
		Plan plan = new Plan(output);

		// optimize and compile plan 
		PactCompiler pc = new PactCompiler();
		OptimizedPlan op = pc.compile(plan);
		
		// return job graph of failing job
		JobGraphGenerator jgg = new JobGraphGenerator();
		return jgg.compileJobGraph(op);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JobGraph getJobGraph() throws Exception {
		
		// path prefix for temp data 
		String pathPrefix = getFilesystemProvider().getURIPrefix()+getFilesystemProvider().getTempDirPath();
		
		// init data source 
		DataSourceContract<PactString, PactString> input = new DataSourceContract<PactString, PactString>(
			MapTestInFormat.class, pathPrefix+"/mapInput");
		input.setFormatParameter("delimiter", "\n");
		input.setDegreeOfParallelism(config.getInteger("MapTest#NoSubtasks", 1));

		// init (working) map task
		MapContract<PactString, PactString, PactString, PactInteger> testMapper = new MapContract<PactString, PactString, PactString, PactInteger>(
			TestMapper.class);
		testMapper.setDegreeOfParallelism(config.getInteger("MapTest#NoSubtasks", 1));

		// init data sink
		DataSinkContract<PactString, PactInteger> output = new DataSinkContract<PactString, PactInteger>(
			MapTestOutFormat.class, pathPrefix + "/result.txt");
		output.setDegreeOfParallelism(1);

		// compose working program
		output.setInput(testMapper);
		testMapper.setInput(input);

		// generate plan
		Plan plan = new Plan(output);

		// optimize and compile plan
		PactCompiler pc = new PactCompiler();
		OptimizedPlan op = pc.compile(plan);

		// return job graph of working job
		JobGraphGenerator jgg = new JobGraphGenerator();
		return jgg.compileJobGraph(op);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void postSubmit() throws Exception {
		String tempDir = getFilesystemProvider().getTempDirPath();
		
		// check result
		compareResultsByLinesInMemory(MAP_RESULT, tempDir+ "/result.txt");

		// delete temp data
		getFilesystemProvider().delete(tempDir+ "/result.txt", true);
		getFilesystemProvider().delete(tempDir+ "/mapInput", true);
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected int getTimeout() {
		// time out for this job is 30 secs
		return 30;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Parameters
	public static Collection<Object[]> getConfigurations() throws FileNotFoundException, IOException {
		LinkedList<Configuration> testConfigs = new LinkedList<Configuration>();

		Configuration config = new Configuration();
		config.setInteger("MapTest#NoSubtasks", 4);
		testConfigs.add(config);

		return toParameterList(RuntimeExceptionFailureITCase.class, testConfigs);
	}

	/**
	 * Input format for test data
	 * 
	 * @author Fabian Hueske (fabian.hueske@tu-berlin.de)
	 *
	 */
	public static class MapTestInFormat extends TextInputFormat<PactString, PactString> {

		@Override
		public boolean readLine(KeyValuePair<PactString, PactString> pair, byte[] line) {

			pair.setKey(new PactString(new String((char) line[0] + "")));
			pair.setValue(new PactString(new String((char) line[2] + "")));

			LOG.debug("Read in: [" + pair.getKey() + "," + pair.getValue() + "]");

			return true;
		}

	}

	/**
	 * Output format for test data
	 * 
	 * @author Fabian Hueske (fabian.hueske@tu-berlin.de)
	 *
	 */
	public static class MapTestOutFormat extends TextOutputFormat<PactString, PactInteger> {

		@Override
		public byte[] writeLine(KeyValuePair<PactString, PactInteger> pair) {
			LOG.debug("Writing out: [" + pair.getKey() + "," + pair.getValue() + "]");

			return (pair.getKey().toString() + " " + pair.getValue().toString() + "\n").getBytes();
		}
	}

	/**
	 * Map stub implementation for working program
	 * 
	 * @author Fabian Hueske (fabian.hueske@tu-berlin.de)
	 *
	 */
	public static class TestMapper extends MapStub<PactString, PactString, PactString, PactInteger> {

		@Override
		public void map(PactString key, PactString value, Collector<PactString, PactInteger> out) {
			
			LOG.debug("Processed: [" + key + "," + value + "]");
			
			if (Integer.parseInt(key.toString()) + Integer.parseInt(value.toString()) < 10) {

				out.collect(value, new PactInteger(Integer.parseInt(key.toString()) + 10));
				
			}
		}
	}
	
	/**
	 * Map stub implementation that fails with a {@link RuntimeException}.
	 * 
	 * @author Fabian Hueske (fabian.hueske@tu-berlin.de)
	 *
	 */
	public static class FailingMapper extends MapStub<PactString, PactString, PactString, PactInteger> {

		@Override
		public void map(PactString key, PactString value, Collector<PactString, PactInteger> out) {
			throw new RuntimeException("This is an expected Test Exception");
		}
		
	}
	
}
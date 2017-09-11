package test.utest
import utest._
import scala.concurrent.Future
import concurrent.duration._
object FrameworkAsyncTests extends utest.TestSuite{
  implicit val ec = utest.framework.ExecutionContext.RunNow
  private val isNative = sys.props("java.vm.name") == "Scala Native"

  def tests = this{
    'hello{
      Future(10)
    }

    'asyncFailures {
      val tests = this {
        "testSuccessAsync" - {
          val p = concurrent.Promise[Int]
          utest.Scheduler.scheduleOnce(2 seconds)(p.success(123))

          // Not supported by Scala Native at the moment.
          // Futures are completed either at the end of the `main` function or immediately.
          // uTest will either consider that the test never completed, or too soon.
          assert(isNative || !p.isCompleted)

          p.future
        }
        "testFailAsync" - {
          val p = concurrent.Promise[Int]
          utest.Scheduler.scheduleOnce(2 seconds)(p.failure(new Exception("Boom")))

          // Not supported by Scala Native at the moment.
          // Futures are completed either at the end of the `main` function or immediately.
          // uTest will either consider that the test never completed, or too soon.
          assert(isNative || !p.isCompleted)

          p.future
        }
        "testSuccess" - {
          Future {
            assert(true)
          }
        }
        "testFail" - {
          Future {
            assert(false)
          }
        }
        "normalSuccess" - {
          assert(true)
        }
        "normalFail" - {
          assert(false)
        }
        "testFailUnexpected" - {
          Future {
            throw new Exception("Lols boom")
          }
        }
        "testCastError" - {
          // These are Fatal in Scala.JS. Ensure they're handled else they freeze SBT.
          Future {
            // This test is disabled until scala-native/scala-native#858 is not fixed.
            assert(!isNative)
            0.asInstanceOf[String]
          }
        }
      }

      utest.runAsync(tests).map { results =>
        val leafResults = results.leaves.toSeq
        assert(leafResults(0).value.isSuccess) // testSuccessAsync
        assert(leafResults(1).value.isFailure) // testFailAsync
        assert(leafResults(2).value.isSuccess) // testSuccess
        assert(leafResults(3).value.isFailure) // testFail
        assert(leafResults(4).value.isSuccess) // normalSuccess
        assert(leafResults(5).value.isFailure) // normalFail
        assert(leafResults(6).value.isFailure) // testFailUnexpected
        assert(leafResults(7).value.isFailure) // testCastError
        leafResults(1).value
      }

    }
  }
}

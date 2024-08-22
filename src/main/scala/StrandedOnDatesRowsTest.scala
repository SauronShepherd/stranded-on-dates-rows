import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{lit, to_date, when}
import org.apache.log4j.{Level, Logger}


object StrandedOnDatesRowsTest {
  def main(args: Array[String]): Unit = {

    // Set the log level to ERROR to reduce logging output
    Logger.getLogger("org").setLevel(Level.ERROR)
    Logger.getLogger("akka").setLevel(Level.ERROR)

    // Create a Spark session
    val codegenPath = this.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
    val spark = SparkSession.builder()
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Create a DataFrame with an invalid date string
    val numRowsList = List(1000000, 5000000, 10000000)
    for(numRows <- numRowsList) {
      val df = (1 to numRows).map { i => (i, "") }
        .toDF("id", "date_str_column")
        .cache()

      // Count to force caching the dataframe before counting nulls
      df.count()

      // Count nulls after applying to_date with empty values
      countNulls(
        "TO_DATE WITH EMPTY VALUES",
        numRows,
        df.withColumn("date_column", to_date($"date_str_column", "yyyy-MM-dd"))
          .withColumn("date_column2", to_date($"date_str_column", "yyyy-MM-dd"))
          .withColumn("date_column3", to_date($"date_str_column", "yyyy-MM-dd"))
      )

      // Count nulls after applying to_date with null values
      countNulls(
        "TO_DATE WITH NULL VALUES",
        numRows,
        df.withColumn("date_column", to_date(
          when($"date_str_column" === lit(""), lit(null)).otherwise($"date_str_column"), "yyyy-MM-dd")
        )
      )

      // Count nulls after no applying to_date to empty values
      countNulls(
        "WITHOUT TO_DATE",
        numRows,
        df.withColumn("date_column",
          when($"date_str_column" === lit(""), lit(null)).otherwise(to_date($"date_str_column", "yyyy-MM-dd"))
        )
      )

    }

  }

  private def countNulls(msg: String, numRows: Long, df: DataFrame): Unit = {
    val time = System.currentTimeMillis()
    df.where("date_column is null").count()
    println(s"$msg ($numRows ROWS)\t${(System.currentTimeMillis() - time)/1000.0}s")
  }
}

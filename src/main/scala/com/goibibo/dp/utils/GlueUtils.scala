/*

- year=2019
- Partition doesn't exist
- Create manifest file and add glue partition
- Partition exists and points to a S3 keyprefix
- List all the files in S3 keyprefix
- Create manifest file which contains all the files in the partition
- Add glue partition
- Partition exists and points to a parquet file
- Create manifest file which contains new files and existing parquet file
- Add glue partition
- Partition exists and points to a manifest file
- Create manifest file which contains combines old manifest and new files
- Add glue partition
addPartition(FieldInfos, manifestFileLocation | "Path")
- Redshift to add partition
- Downtime/Queue
- Athena
- Parallel queries limit
- Integrate Glue as HiveCatalog in Spark
- Use Glue APIs

- Don't use Spark but use Spark's internal GlueCatalog APIs

updatePartition
dropPartition
checkIfPartitionExists
getPartition(tableName, patitionFields) : (List[S3Paths], ParitionDetails(Path|File|Manifest))

createTable
dropTable
alterTableAddField
alterTableDropField

*/

package com.goibibo.dp.utils

import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.regions.Region

import scala.util.Try
import scala.reflect.ClassTag
import software.amazon.awssdk.services.glue.model.SerDeInfo

import java.util.Date
import java.sql.Timestamp

import software.amazon.awssdk.services.glue.model.{Column => GlueColumn}
import software.amazon.awssdk.services.glue.model.{
  CreatePartitionRequest,
  DeletePartitionRequest,
  CreateTableRequest,
  DeleteTableRequest,
  TableInput,
  StorageDescriptor
}
import collection.JavaConverters._


object GlueUtilsTypes {
  
  trait PartitionDetails {}
  case class ManifestPartition(manifestFile:String) extends PartitionDetails
  case class KeyPrefixPartition(path:String) extends PartitionDetails
  
  case class FieldInfo(fieldName:String, fieldType:String)
  case class FieldInfoValue[T:ClassTag](fieldName:String, fieldValue:T) {
    def fieldType = GlueUtilsPrivate.glueType[T]()
  }
  
  case class TableDetails(name:String, db: String)
  
  trait GlueFileFormat {
    def inputFormat:String {}
    def outputFormat:String {}
    def serializationLibrary:String {}
    def serializationFormat:String {}
    def numberOfBuckets:Int {}
  }

  object ParquetFormat extends GlueFileFormat {
    def inputFormat:String = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    def outputFormat:String = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"
    def serializationLibrary:String = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"
    def serializationFormat:String = "1"
    def numberOfBuckets:Int = -1
  }

  type Fields = Seq[FieldInfo]
  type S3FilesList = Seq[String]
  
  val TABLE_TYPE_EXTERNAL = "EXTERNAL_TABLE"
}

object GlueUtils {
  import GlueUtilsTypes._
  import GlueUtilsPrivate._
  
  def createPartition(table:TableDetails, partitions:Fields, location:String)
  (implicit glueClient:GlueClient):Try[Unit] = {
    Try{
      //add partition here
    }
  }

  def dropPartition() (implicit glueClient:GlueClient):Try[Unit] = {
    Try{
      //Implementation here
    }
  }

  def checkIfPartitionExists() (implicit glueClient:GlueClient):Try[Unit] = {
    Try{
      //Implementation here
    }
  }

  def getPartition(FieldInfos:Fields):(S3FilesList, PartitionDetails) = {
    ( Seq[String](), KeyPrefixPartition("") )
  }
  
  def createTable(
    tableDetails:TableDetails, partitionKeys: Fields, 
    fields:Fields, location:String, fileFormat: GlueFileFormat
  ) 
  (implicit glueClient:GlueClient):Try[Unit] = {
    
    Try{
      val tableStorage = createStorageDescriptor(location, fields, fileFormat)
      val tableInput:TableInput = TableInput.builder()
      .name(tableDetails.name)
      .tableType(TABLE_TYPE_EXTERNAL)
      .partitionKeys( fieldInfoToGlueType(partitionKeys):_* )
      .storageDescriptor(tableStorage)
      .build()
      
      val createTableRequest = CreateTableRequest.builder()
      .databaseName(tableDetails.db)
      .tableInput(tableInput)
      .build()
      glueClient.createTable(createTableRequest)
    }
  }

  def createStorageDescriptor(location:String, fields: Fields, fileFormat: GlueFileFormat):StorageDescriptor = {
    StorageDescriptor.builder()
      .location(location)
      .columns(fieldInfoToGlueType(fields):_*)
      .inputFormat(fileFormat.inputFormat)
      .outputFormat(fileFormat.outputFormat)
      .numberOfBuckets(fileFormat.numberOfBuckets)
      .serdeInfo(
        SerDeInfo.builder()
          .serializationLibrary(fileFormat.serializationLibrary)
          .parameters(Map[String,String]("serialization.format" -> fileFormat.serializationFormat).asJava)
          .build()
      )
      .build()
  }

  def dropTable() (implicit glueClient:GlueClient):Try[Unit] = {
    Try{
      //Implementation here
    }
  }
  def alterTableAddField() (implicit glueClient:GlueClient):Try[Unit] = {
    Try{
      //Implementation here
    }
  }
  def alterTableDropField() (implicit glueClient:GlueClient):Try[Unit] = {
    Try{
      
    }
  }
  
  def createGlueClient(region:String = "ap-south-1"):GlueClient = {
    val regionO = Region.of(region)
    GlueClient.builder().region( regionO ).build()
  }
}

object GlueUtilsPrivate {
  import GlueUtilsTypes._
  
  def glueType[T:ClassTag]():String = {
    
    val runtimeClassOfT = implicitly[ClassTag[T]].runtimeClass
    import org.apache.kafka.common.utils.Java
    if( runtimeClassOfT == classOf[String])     "STRING"
    else if( runtimeClassOfT == classOf[Int])   "INT"
    else if( runtimeClassOfT == classOf[Long] ) "LONG"
    else if( runtimeClassOfT == classOf[Float] ) "FLOAT"
    else if( runtimeClassOfT == classOf[Double] ) "DOUBLE"
    else if( runtimeClassOfT == classOf[Date] ) "DATE"
    /* TODO: Not adding additional types as rest of the types are supported in Glue as a partition type */
    else "UNKNOWN"
  }
  
  def fieldInfoToGlueType(fields:Fields):Seq[GlueColumn] = {
    fields.map( a => GlueColumn.builder.name(a.fieldName).`type`(a.fieldType).build() )
  }
  
}
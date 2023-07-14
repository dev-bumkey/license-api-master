/*
 * Harbor API
 * These APIs provide services for manipulating Harbor project.
 *
 * OpenAPI spec version: 2.0
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package run.acloud.commons.client.harbor.v2.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SystemInfo
 */
@jakarta.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2022-06-22T08:06:19.711Z")
public class SystemInfo {
  @SerializedName("storage")
  private List<Storage> storage = null;

  public SystemInfo storage(List<Storage> storage) {
    this.storage = storage;
    return this;
  }

  public SystemInfo addStorageItem(Storage storageItem) {
    if (this.storage == null) {
      this.storage = new ArrayList<Storage>();
    }
    this.storage.add(storageItem);
    return this;
  }

   /**
   * The storage of system.
   * @return storage
  **/
  @Schema(description = "The storage of system.")
  public List<Storage> getStorage() {
    return storage;
  }

  public void setStorage(List<Storage> storage) {
    this.storage = storage;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SystemInfo systemInfo = (SystemInfo) o;
    return Objects.equals(this.storage, systemInfo.storage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(storage);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SystemInfo {\n");
    
    sb.append("    storage: ").append(toIndentedString(storage)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}


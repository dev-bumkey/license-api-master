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


package run.acloud.commons.vo;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * UserCreationReq
 */
@Schema(title="Harbor User Request Model", description="Harbor User Request Model")
public class HarborUserReqVO {
  @SerializedName("email")
  private String email = null;

  @SerializedName("realname")
  private String realname = null;

  @SerializedName("comment")
  private String comment = null;

  @SerializedName("password")
  private String password = null;

  @SerializedName("username")
  private String username = null;

  public HarborUserReqVO email(String email) {
    this.email = email;
    return this;
  }

   /**
   * Get email
   * @return email
  **/
  @Schema(description = "")
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public HarborUserReqVO realname(String realname) {
    this.realname = realname;
    return this;
  }

   /**
   * Get realname
   * @return realname
  **/
  @Schema(description = "")
  public String getRealname() {
    return realname;
  }

  public void setRealname(String realname) {
    this.realname = realname;
  }

  public HarborUserReqVO comment(String comment) {
    this.comment = comment;
    return this;
  }

   /**
   * Get comment
   * @return comment
  **/
  @Schema(description = "")
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public HarborUserReqVO password(String password) {
    this.password = password;
    return this;
  }

   /**
   * Get password
   * @return password
  **/
  @Schema(description = "")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public HarborUserReqVO username(String username) {
    this.username = username;
    return this;
  }

   /**
   * Get username
   * @return username
  **/
  @Schema(description = "")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HarborUserReqVO userCreationReq = (HarborUserReqVO) o;
    return Objects.equals(this.email, userCreationReq.email) &&
        Objects.equals(this.realname, userCreationReq.realname) &&
        Objects.equals(this.comment, userCreationReq.comment) &&
        Objects.equals(this.password, userCreationReq.password) &&
        Objects.equals(this.username, userCreationReq.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, realname, comment, password, username);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserCreationReq {\n");
    
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    realname: ").append(toIndentedString(realname)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

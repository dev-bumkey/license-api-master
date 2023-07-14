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
 * UserResp
 */
@Schema(title="Harbor User Response Model", description="Harbor User Response Model")
public class HarborUserRespVO {
  @SerializedName("email")
  private String email = null;

  @SerializedName("realname")
  private String realname = null;

  @SerializedName("comment")
  private String comment = null;

  @SerializedName("user_id")
  private Integer userId = null;

  @SerializedName("username")
  private String username = null;

  public HarborUserRespVO email(String email) {
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

  public HarborUserRespVO realname(String realname) {
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

  public HarborUserRespVO comment(String comment) {
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

  public HarborUserRespVO userId(Integer userId) {
    this.userId = userId;
    return this;
  }

   /**
   * Get userId
   * @return userId
  **/
  @Schema(description = "")
  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public HarborUserRespVO username(String username) {
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
    HarborUserRespVO userResp = (HarborUserRespVO) o;
    return Objects.equals(this.email, userResp.email) &&
        Objects.equals(this.realname, userResp.realname) &&
        Objects.equals(this.comment, userResp.comment) &&
        Objects.equals(this.userId, userResp.userId) &&
        Objects.equals(this.username, userResp.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, realname, comment, userId, username);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserResp {\n");
    
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    realname: ").append(toIndentedString(realname)).append("\n");
    sb.append("    comment: ").append(toIndentedString(comment)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
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

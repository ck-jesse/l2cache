package com.coy.l2cache.test;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Objects;

@Data
@EqualsAndHashCode // 方式一：通过lombok注解重写 hashCode() 和 equals()
public class UserDTO implements Serializable {

    public UserDTO() {
    }

    public UserDTO(String name, String userId) {
        this.name = name;
        this.userId = userId;
    }

    private String name;
    private String userId;

    // 方式二：显示重写 hashCode() 和 equals()
/*
public int hashCode() {
        return Objects.hashCode(name) ^ Objects.hashCode(userId);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof UserDTO) {
            UserDTO dto = (UserDTO) obj;
            if (Objects.equals(name, dto.getName()) && Objects.equals(userId, dto.getUserId())) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return "[name=" + name + ",userId=" + userId + "]";
    }*/

}

/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.kwatee.agiledeployment.repository.dto.ApplicationParameterDto;
import net.kwatee.agiledeployment.repository.dto.UserDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.entity.ApplicationParameter;
import net.kwatee.agiledeployment.repository.entity.Authority;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;
import net.kwatee.agiledeployment.repository.entity.User;

import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

	/**
	 * 
	 * @param users
	 * @return user dtos
	 */
	public Collection<UserDto> toShortUserDtos(Collection<User> users) {
		List<User> sortedUsers = new ArrayList<User>(users);
		Collections.sort(sortedUsers, new Comparator<User>() {

			@Override
			public int compare(User user1, User user2) {
				return user1.getLogin().compareToIgnoreCase(user2.getLogin());
			}
		});
		Collection<UserDto> dtos = new ArrayList<>(users.size());
		for (User user : sortedUsers) {
			dtos.add(toShortUserDto(user));
		}
		return dtos;
	}

	/**
	 * @param user
	 * @return user dto
	 */
	public UserDto toUserDto(User user) {
		UserDto dto = toShortUserDto(user);
		dto.setEmail(user.getEmail());
		dto.setOperator(user.hasAuthority(Authority.ROLE_OPERATOR));
		dto.setSrm(user.hasAuthority(Authority.ROLE_SRM));
		dto.setAdmin(user.hasAuthority(Authority.ROLE_ADMIN));

		return dto;
	}

	/**
	 * @param user
	 * @param dto
	 *            If null a new dto is created
	 * @return UserShortDto
	 */
	private UserDto toShortUserDto(User user) {
		UserDto dto = new UserDto();
		dto.setName(user.getLogin());
		dto.setDescription(user.getDescription());
		dto.setDisabled(user.isDisabled());
		return dto;
	}

	/**
	 * 
	 * @param globalVariables
	 * @return variable dtos
	 */
	public Collection<VariableDto> toGlobalVariableDtos(Collection<SystemProperty> globalVariables) {
		Collection<VariableDto> dtos = new ArrayList<>(globalVariables.size());
		for (SystemProperty p : globalVariables) {
			if (!p.isHidden()) {
				VariableDto dto = new VariableDto();
				dto.setName(p.getName());
				dto.setValue(p.getValue());
				dto.setDescription(p.getDescription());
				dtos.add(dto);
			}
		}
		return dtos;
	}

	/**
	 * 
	 * @param parameters
	 * @return
	 */
	public ApplicationParameterDto toParametersDto(ApplicationParameter parameters) {
		ApplicationParameterDto dto = new ApplicationParameterDto();
		dto.setTitle(parameters.getTitle());
		Set<String> extensions = parameters.getExcludedExtensionsAsSet();
		Collection<String> excludedExtensions = new ArrayList<>();
		if (extensions != null) {
			ArrayList<String> sortedExtensions = new ArrayList<>(extensions);
			Collections.sort(sortedExtensions);
			for (String extension : sortedExtensions) {
				excludedExtensions.add(extension);
			}
		}
		dto.setExcludedExtensions(excludedExtensions);
		return dto;
	}
}

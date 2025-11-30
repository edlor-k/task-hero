package ru.taskhero.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.taskhero.userservice.dto.ParentResponseDto;
import ru.taskhero.userservice.entity.Child;
import ru.taskhero.userservice.entity.Parent;

import java.util.List;
import java.util.UUID;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {UserMapper.class})
public interface ParentMapper {

    @Mapping(target = "userId", expression = "java(parent.getUser() != null ? parent.getUser().getId() : null)")
    @Mapping(target = "childIds", expression = "java(childrenToChildIds(parent.getChildren()))")
    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "surname", target = "surname")
    ParentResponseDto toParentResponseDto(Parent parent);


    default List<UUID> childrenToChildIds(List<Child> children) {
        if (children == null) {
            return List.of();
        }
        return children.stream().map(Child::getId).toList();
    }
}

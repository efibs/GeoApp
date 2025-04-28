using GeoAppAPI.Models;

namespace GeoAppAPI.Dtos.Assemblers;

public static class DataDtoAssembler
{
    public static IEnumerable<DataDto> AssembleDtos(IEnumerable<Data> models)
    {
        return models.Select(AssembleDto);
    }
    
    public static DataDto AssembleDto(Data model)
    {
        return new DataDto
        {
            Latitude = model.Latitude,
            Longitude = model.Longitude,
            Steps = model.Steps,
            Timestamp = model.Timestamp,
        };
    }
    
    public static IEnumerable<Data> AssembleModels(IEnumerable<DataDto> dtos)
    {
        return dtos.Select(AssembleModel);
    }
    
    public static Data AssembleModel(DataDto dto)
    {
        return new Data
        {
            Latitude = dto.Latitude,
            Longitude = dto.Longitude,
            Steps = dto.Steps,
            Timestamp = dto.Timestamp.DateTime,
        };
    }
}
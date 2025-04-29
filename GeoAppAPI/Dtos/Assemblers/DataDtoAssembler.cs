using GeoAppAPI.Dtos;
using GeoAppAPI.Models;

namespace GeoAppAPI.Dtos.Assemblers;

public static class DataDtoAssembler
{
    public static List<DataDto> AssembleDtos(IEnumerable<Data> models)
    {
        return models.Select(AssembleDto).ToList();
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
    
    public static List<Data> AssembleModels(IEnumerable<DataDto> dtos)
    {
        return dtos.Select(AssembleModel).ToList();
    }
    
    public static Data AssembleModel(DataDto dto)
    {
        return new Data
        {
            Latitude = dto.Latitude,
            Longitude = dto.Longitude,
            Steps = dto.Steps,
            Timestamp = dto.Timestamp.UtcDateTime,
        };
    }
}
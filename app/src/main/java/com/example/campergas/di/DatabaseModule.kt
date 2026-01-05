package com.example.campergas.di

import android.content.Context
import androidx.room.Room
import com.example.campergas.data.local.db.CamperGasDatabase
import com.example.campergas.data.local.db.FuelMeasurementDao
import com.example.campergas.data.local.db.GasCylinderDao
import com.example.campergas.data.local.vehicle.VehicleDao
import com.example.campergas.data.local.vehicle.VehicleDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module for database components.
 *
 * Provides singleton instances of Room databases and their DAOs for
 * gas cylinder tracking and vehicle configuration.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the main CamperGas Room database instance.
     *
     * @param context Application context
     * @return Singleton CamperGasDatabase instance
     */
    @Provides
    @Singleton
    fun provideCamperGasDatabase(@ApplicationContext context: Context): CamperGasDatabase {
        return Room.databaseBuilder(
            context,
            CamperGasDatabase::class.java,
            CamperGasDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    /**
     * Provides DAO for gas cylinder operations.
     *
     * @param database CamperGasDatabase instance
     * @return GasCylinderDao instance
     */
    @Provides
    fun provideGasCylinderDao(database: CamperGasDatabase): GasCylinderDao {
        return database.gasCylinderDao()
    }

    /**
     * Provides DAO for fuel measurement operations.
     *
     * @param database CamperGasDatabase instance
     * @return FuelMeasurementDao instance
     */
    @Provides
    fun provideFuelMeasurementDao(database: CamperGasDatabase): FuelMeasurementDao {
        return database.fuelMeasurementDao()
    }

    /**
     * Provides the vehicle configuration Room database instance.
     *
     * Includes migration support from version 1 to 2.
     *
     * @param context Application context
     * @return Singleton VehicleDatabase instance
     */
    @Provides
    @Singleton
    fun provideVehicleDatabase(@ApplicationContext context: Context): VehicleDatabase {
        return Room.databaseBuilder(
            context,
            VehicleDatabase::class.java,
            "vehicle_database"
        )
            .addMigrations(VehicleDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    /**
     * Provides DAO for vehicle configuration operations.
     *
     * @param database VehicleDatabase instance
     * @return VehicleDao instance
     */
    @Provides
    fun provideVehicleDao(database: VehicleDatabase): VehicleDao {
        return database.vehicleDao()
    }

}
